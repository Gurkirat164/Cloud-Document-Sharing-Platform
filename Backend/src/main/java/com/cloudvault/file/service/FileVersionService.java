package com.cloudvault.file.service;

import com.cloudvault.activity.service.ActivityLogService;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.FileVersion;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import com.cloudvault.domain.enums.Role;
import com.cloudvault.file.dto.FileVersionResponse;
import com.cloudvault.file.dto.RestoreVersionRequest;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.file.repository.FileVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for file version management.
 *
 * <p>Version lifecycle:
 * <ol>
 *   <li><b>recordNewVersion</b> — called after every successful upload or restore to
 *       create a DB record for the new S3 version and update the "current" flag.</li>
 *   <li><b>getVersionHistory</b> — read-only; returns all versions newest-first.</li>
 *   <li><b>restoreVersion</b> — copies an old S3 version to the current key, then
 *       calls recordNewVersion.  S3 copy is attempted first; only if S3 succeeds
 *       is a DB record created (preventing orphaned version entries).</li>
 *   <li><b>deleteVersion</b> — permanently removes an S3 version and its DB record.
 *       The current version is protected — clients must soft-delete the file instead.</li>
 * </ol>
 *
 * <p>PREREQUISITE: S3 bucket versioning must be enabled — every PUT must return a
 * non-null versionId for version tracking to work correctly.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FileVersionService {

    private final FileVersionRepository fileVersionRepository;
    private final FileRepository        fileRepository;
    private final S3Service             s3Service;
    private final ActivityLogService    activityLogService;

    // ── Write path ───────────────────────────────────────────────────────────

    /**
     * Records a new version entry for a file after a successful S3 PUT.
     *
     * <p>Steps (all inside one transaction):
     * <ol>
     *   <li>Determine the next version number (max existing + 1, or 1 if this is
     *       the first upload).</li>
     *   <li>Mark all existing versions as non-current.</li>
     *   <li>Create and save the new {@link FileVersion} with
     *       {@code isCurrentVersion = true}.</li>
     *   <li>Update {@link File#getCurrentS3VersionId()} and save the file.</li>
     * </ol>
     *
     * @param file          the parent file entity
     * @param s3VersionId   the S3 version ID returned by the PUT operation
     * @param uploadedBy    user who triggered the upload or restore
     * @return the newly created {@link FileVersionResponse}
     */
    public FileVersionResponse recordNewVersion(File file, String s3VersionId, User uploadedBy) {
        return recordNewVersion(file, s3VersionId, uploadedBy, null);
    }

    /**
     * Internal overload used by restoreVersion() to capture the restored-from number.
     */
    private FileVersionResponse recordNewVersion(File file, String s3VersionId,
                                                  User uploadedBy, Integer restoredFromVersion) {
        // 1. Determine the next sequential version number.
        int nextVersionNumber = fileVersionRepository
                .findTopByFileIdOrderByVersionNumberDesc(file.getId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        // 2. Flip all existing versions to isCurrentVersion = false.
        List<FileVersion> existingVersions =
                fileVersionRepository.findAllByFileIdOrderByVersionNumberDesc(file.getId());
        existingVersions.forEach(v -> v.setCurrentVersion(false));
        fileVersionRepository.saveAll(existingVersions);

        // 3. Create the new current version record.
        FileVersion newVersion = FileVersion.builder()
                .file(file)
                .versionNumber(nextVersionNumber)
                .s3VersionId(s3VersionId)
                .s3Key(file.getS3Key())
                .originalName(file.getOriginalName())
                .sizeBytes(file.getSizeBytes())
                .mimeType(file.getMimeType())
                .checksum(file.getChecksum())
                .uploadedBy(uploadedBy)
                .isCurrentVersion(true)
                .restoredFromVersion(restoredFromVersion)
                .build();

        FileVersion saved = fileVersionRepository.save(newVersion);

        // 4. Update the denormalised version pointer on the File entity.
        file.setCurrentS3VersionId(s3VersionId);
        fileRepository.save(file);

        log.info("New version {} recorded for file {} by user {}",
                nextVersionNumber, file.getUuid(), uploadedBy.getEmail());

        return FileVersionResponse.from(saved);
    }

    // ── Read path ────────────────────────────────────────────────────────────

    /**
     * Returns the full version history for a file, newest-first.
     *
     * <p>Access rules:
     * <ul>
     *   <li>File owner — always allowed.</li>
     *   <li>ADMIN — always allowed.</li>
     *   <li>Any other user with an active file permission — allowed
     *       (they can see the file, so they can see its history).</li>
     *   <li>Everyone else — {@link AccessDeniedException}.</li>
     * </ul>
     *
     * @param fileUuid    public UUID of the file
     * @param currentUser authenticated caller
     * @return list of {@link FileVersionResponse}, newest first
     */
    @Transactional(readOnly = true)
    public List<FileVersionResponse> getVersionHistory(String fileUuid, User currentUser) {
        File file = findFileOrThrow(fileUuid);
        checkReadAccess(file, currentUser);

        return fileVersionRepository
                .findAllByFileIdOrderByVersionNumberDesc(file.getId())
                .stream()
                .map(FileVersionResponse::from)
                .toList();
    }

    /**
     * Returns the total number of versions recorded for a file.
     *
     * @param fileUuid public UUID of the file
     * @return version count (0 if the file has never been versioned)
     */
    @Transactional(readOnly = true)
    public int getVersionCount(String fileUuid) {
        File file = findFileOrThrow(fileUuid);
        return fileVersionRepository.countByFileId(file.getId());
    }

    // ── Mutating operations ──────────────────────────────────────────────────

    /**
     * Restores a file to a specific previous version.
     *
     * <p>Restoration works by making a new S3 version that has the same content
     * as the target version. The sequence is:
     * <ol>
     *   <li>Validate preconditions (ownership, not-deleted, target version exists
     *       and is not already current).</li>
     *   <li>Call {@link S3Service#copyVersion} — S3 copy is attempted <em>first</em>.
     *       If this fails, no DB changes are made.</li>
     *   <li>Only if S3 copy succeeds: call {@link #recordNewVersion} to create the
     *       new DB version record (with {@code restoredFromVersion} set) and update
     *       the File's current S3 version pointer.</li>
     *   <li>Log a {@code FILE_RESTORE} activity event.</li>
     * </ol>
     *
     * @param fileUuid    public UUID of the file to restore
     * @param request     contains the target version number
     * @param currentUser authenticated caller (must be file owner or ADMIN)
     * @return the {@link FileVersionResponse} of the newly created restore version
     * @throws ResourceNotFoundException  if file or target version does not exist
     * @throws AccessDeniedException      if caller is not owner or ADMIN
     * @throws IllegalStateException      if the file is soft-deleted
     * @throws IllegalArgumentException   if the target version is already current
     */
    public FileVersionResponse restoreVersion(String fileUuid,
                                               RestoreVersionRequest request,
                                               User currentUser) {
        File file = findFileOrThrow(fileUuid);
        checkWriteAccess(file, currentUser);

        if (Boolean.TRUE.equals(file.getIsDeleted())) {
            throw new IllegalStateException("Cannot restore version of a deleted file");
        }

        FileVersion targetVersion = fileVersionRepository
                .findByFileIdAndVersionNumber(file.getId(), request.getVersionNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version not found: " + request.getVersionNumber()));

        if (targetVersion.isCurrentVersion()) {
            throw new IllegalArgumentException("This version is already the current version");
        }

        // S3 copy is attempted FIRST — if S3 fails, no DB record is created.
        String newS3VersionId = s3Service.copyVersion(
                targetVersion.getS3Key(),
                targetVersion.getS3VersionId(),
                file.getS3Key()
        );

        // S3 copy succeeded — now create the DB version record atomically.
        // Update size/name/mimeType on the File to reflect the restored content.
        file.setSizeBytes(targetVersion.getSizeBytes());
        file.setOriginalName(targetVersion.getOriginalName());
        file.setMimeType(targetVersion.getMimeType());
        file.setChecksum(targetVersion.getChecksum());

        FileVersionResponse restored = recordNewVersion(
                file, newS3VersionId, currentUser, request.getVersionNumber());

        activityLogService.log(currentUser, file, EventType.FILE_RESTORE, null);

        log.info("File {} restored to version {} by user {}",
                fileUuid, request.getVersionNumber(), currentUser.getEmail());

        return restored;
    }

    /**
     * Permanently deletes a specific non-current version of a file.
     *
     * <p>The current version cannot be deleted via this method — the caller must
     * soft-delete the entire file instead. This is enforced to prevent accidentally
     * removing the only accessible copy of a file.
     *
     * <p>The S3 version is deleted before the DB record. If S3 deletion fails,
     * the exception propagates and the DB record is NOT removed.  This is the
     * stricter invariant: we prefer an orphaned DB record over orphaned S3 bytes.
     *
     * @param fileUuid      public UUID of the file
     * @param versionNumber the specific version number to delete permanently
     * @param currentUser   authenticated caller (must be file owner or ADMIN)
     * @throws ResourceNotFoundException if file or version does not exist
     * @throws AccessDeniedException     if caller is not owner or ADMIN
     * @throws IllegalArgumentException  if caller attempts to delete the current version
     */
    public void deleteVersion(String fileUuid, int versionNumber, User currentUser) {
        File file = findFileOrThrow(fileUuid);
        checkWriteAccess(file, currentUser);

        FileVersion version = fileVersionRepository
                .findByFileIdAndVersionNumber(file.getId(), versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version not found: " + versionNumber));

        if (version.isCurrentVersion()) {
            throw new IllegalArgumentException(
                    "Cannot delete the current version. Delete the file instead.");
        }

        // Delete from S3 first — if this throws, the DB record stays intact.
        s3Service.deleteVersion(version.getS3Key(), version.getS3VersionId());

        // S3 deletion confirmed — now remove the DB record.
        fileVersionRepository.delete(version);

        log.info("Version {} of file {} permanently deleted by user {}",
                versionNumber, fileUuid, currentUser.getEmail());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private File findFileOrThrow(String fileUuid) {
        return fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File not found: " + fileUuid));
    }

    /**
     * Read access: owner OR ADMIN OR any user with an active permission on this file.
     * Throws {@link AccessDeniedException} if none of those conditions are met.
     */
    private void checkReadAccess(File file, User currentUser) {
        boolean isOwner = file.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            // Shared users with active permissions can view the version history.
            // A lightweight existence check via JPQL avoids pulling in a separate
            // FilePermissionRepository dependency.
            boolean hasPermission = fileVersionRepository
                    .existsActivePermission(file.getId(), currentUser.getId());
            if (!hasPermission) {
                throw new AccessDeniedException(
                        "You do not have access to this file's version history");
            }
        }
    }

    /**
     * Write access: owner OR ADMIN only. Shared users can view but not restore/delete.
     */
    private void checkWriteAccess(File file, User currentUser) {
        boolean isOwner = file.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException(
                    "Only the file owner or an admin can perform this action");
        }
    }
}
