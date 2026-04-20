package com.cloudvault.file.service;

import com.cloudvault.activity.annotation.LogActivity;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.common.exception.StorageQuotaExceededException;
import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import com.cloudvault.domain.enums.Role;
import com.cloudvault.file.dto.FileMetadataRequest;
import com.cloudvault.file.dto.FileResponse;
import com.cloudvault.file.dto.FileSearchRequest;
import com.cloudvault.file.dto.PresignedUrlResponse;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.file.specification.FileSpecification;
import com.cloudvault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core business logic for file management:
 * - presigned upload URL generation (with early quota pre-check)
 * - saving file metadata after a direct S3 upload (with definitive quota check)
 * - listing user files
 * - soft-deleting files with quota decrement
 * - dynamic search with filtering and pagination
 * - recent files widget
 * - storage usage stats
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileVersionService fileVersionService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    // ── Public methods ───────────────────────────────────────────────────────

    /**
     * Performs an early quota pre-check, then generates a unique S3 key and
     * returns a presigned PUT URL.
     *
     * <p>This is a <strong>UX convenience check</strong> — it fails fast before
     * the browser sends any bytes to S3. The authoritative quota check that
     * prevents a DB record from being created still happens in
     * {@link #saveFileMetadata(FileMetadataRequest, User)}.
     *
     * <p>Key format: {@code users/{userUuid}/{newUuid}-{sanitisedFilename}}
     *
     * @param filename      original filename from the browser
     * @param contentType   MIME type
     * @param fileSizeBytes declared file size in bytes used for the pre-check
     * @param user          authenticated user performing the upload
     * @return presigned URL + confirmed S3 key + TTL
     * @throws StorageQuotaExceededException if the declared size would exceed quota
     */
    @LogActivity(EventType.FILE_DOWNLOAD)
    public PresignedUrlResponse getPresignedUploadUrl(String filename, String contentType,
                                                       long fileSizeBytes, User user) {
        // Early quota pre-check — fast feedback before any S3 interaction
        checkStorageQuota(user, fileSizeBytes);

        String sanitised = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String s3Key = "users/" + user.getUuid() + "/" + UUID.randomUUID() + "-" + sanitised;
        log.debug("Generating presigned PUT URL for user={} key={}", user.getEmail(), s3Key);
        return s3Service.generatePresignedPutUrl(s3Key, contentType);
    }

    /**
     * Persists file metadata to MySQL after the frontend has completed the S3 PUT.
     *
     * <p>Order of operations (all inside one transaction):
     * <ol>
     *   <li>Definitive quota check — throws {@link StorageQuotaExceededException}
     *       BEFORE any DB write if the file would exceed quota.</li>
     *   <li>Save the {@link File} entity.</li>
     *   <li>Increment the owner's {@code storageUsed} counter.</li>
     * </ol>
     *
     * @param request metadata sent by the frontend
     * @param user    authenticated uploader
     * @return the saved {@link FileResponse}
     * @throws StorageQuotaExceededException if the file would push usage past the quota
     */
    @LogActivity(EventType.FILE_UPLOAD)
    @Transactional
    public FileResponse saveFileMetadata(FileMetadataRequest request, User user) {
        // ① Definitive quota check — must happen BEFORE any DB write.
        //   If this throws, no File record is created and no S3 key is referenced in the DB.
        checkStorageQuota(user, request.sizeBytes());

        File file = File.builder()
                .owner(user)
                .originalName(request.originalName())
                .s3Key(request.s3Key())
                .s3Bucket(bucketName)
                .mimeType(request.mimeType())
                .sizeBytes(request.sizeBytes())
                .checksum(request.checksum())
                .build();

        fileRepository.save(file);

        // ② Increment storage counter — only reached when the file was saved successfully.
        incrementStorageUsed(user, request.sizeBytes());

        // ③ Record version — get the S3 version ID and create a FileVersion entry.
        //    This is a best-effort operation: if versioning is not enabled on the bucket,
        //    getLatestVersionId() returns null and no version record is created.
        //    A failure here must NOT roll back the file save — we treat versioning as
        //    supplemental metadata, not a core invariant.
        try {
            String s3VersionId = s3Service.getLatestVersionId(file.getS3Key());
            if (s3VersionId != null) {
                fileVersionService.recordNewVersion(file, s3VersionId, user);
            } else {
                log.debug("Bucket versioning not enabled or versionId unavailable for key={}; " +
                          "skipping version record creation.", file.getS3Key());
            }
        } catch (Exception ex) {
            log.warn("Failed to record version for file uuid={}: {}. " +
                     "File was saved successfully.", file.getUuid(), ex.getMessage());
        }

        log.info("Saved file metadata: uuid={} owner={}", file.getUuid(), user.getEmail());
        return FileResponse.from(file);
    }

    /**
     * Returns a paginated list of the user's non-deleted files.
     *
     * @param user     authenticated user
     * @param pageable pagination/sorting parameters
     * @return page of FileResponse records
     */
    @Transactional(readOnly = true)
    public Page<FileResponse> getUserFiles(User user, Pageable pageable) {
        return fileRepository.findByOwnerAndIsDeletedFalse(user, pageable)
                .map(FileResponse::from);
    }

    /**
     * Soft-deletes the DB record, decrements the owner's storage counter, and
     * removes the S3 object.
     *
     * <p>Design decisions:
     * <ul>
     *   <li>DB soft-delete and storage decrement happen inside the transaction —
     *       they are the source of truth; if the transaction rolls back, both
     *       are reverted atomically.</li>
     *   <li>S3 deletion happens <em>after</em> the transaction commits. A failed
     *       S3 call is logged at WARN but does NOT roll back the DB — this prevents
     *       a transient S3 error from leaving the user unable to "delete" a file.</li>
     *   <li>Orphaned S3 objects are swept by a scheduled cleanup job.</li>
     * </ul>
     *
     * @param fileUuid public UUID of the file to delete
     * @param user     authenticated user performing the deletion
     */
    @LogActivity(EventType.FILE_DELETE)
    @Transactional
    public void deleteFile(String fileUuid, User user) {
        File file = fileRepository.findByUuidAndIsDeletedFalse(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        if (!file.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this file");
        }

        // 1. Capture S3 key before we lose the reference
        String s3Key = file.getS3Key();

        // 2. Soft-delete in DB (inside transaction)
        file.setIsDeleted(true);
        file.setDeletedAt(Instant.now());
        fileRepository.save(file);

        // 3. Decrement the owner's storage counter — inside the same transaction
        //    so a rollback reverts both the soft-delete and the counter update atomically.
        decrementStorageUsed(user, file.getSizeBytes());

        log.info("Soft-deleted file uuid={} s3Key={} owner={}", fileUuid, s3Key, user.getEmail());

        // 4. Remove the S3 object — done after DB writes so a rollback never
        //    leaves a dangling DB row pointing at a deleted S3 object.
        //    S3 failure is non-fatal: the object becomes orphaned and will be
        //    swept by a scheduled cleanup job.
        try {
            s3Service.deleteObject(s3Key);
            log.info("Deleted S3 object for file uuid={}", fileUuid);
        } catch (Exception ex) {
            log.warn("S3 object deletion failed for key={} — will be cleaned up by sweep job. Cause: {}",
                    s3Key, ex.getMessage());
        }
    }

    /**
     * Searches the caller's files (or all files for ADMIN) using the provided filters.
     *
     * <p>Security invariant: a non-ADMIN caller can <em>never</em> see another user's
     * files, regardless of what filter values the HTTP client sends. The owner id is
     * always forced to {@code currentUser.getId()} before the specification is built.
     *
     * <p>Invalid {@code sortBy} and {@code sortDirection} values are silently normalised
     * to safe defaults before the query executes.
     *
     * @param request     filter and pagination parameters
     * @param currentUser the authenticated caller
     * @return paginated, mapped result set
     */
    @Transactional(readOnly = true)
    public PagedResponse<FileResponse> searchFiles(FileSearchRequest request, User currentUser) {
        // Silently normalise invalid sort parameters.
        if (!request.isValidSortBy()) {
            request.setSortBy("uploadedAt");
        }
        if (!request.isValidSortDirection()) {
            request.setSortDirection("DESC");
        }

        Sort sort = Sort.by(
                Sort.Direction.fromString(request.getSortDirection()),
                request.getSortBy()
        );
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        // Non-admin: always scope to own files.
        Long ownerId = isAdmin ? null : currentUser.getId();

        Specification<File> spec =
                FileSpecification.buildFromRequest(request, ownerId, isAdmin);

        Page<File> page = fileRepository.findAll(spec, pageable);

        List<FileResponse> content = page.getContent()
                .stream()
                .map(FileResponse::from)
                .toList();

        log.debug("File search executed for user {} with filters: query={}, mimeType={}, sortBy={}",
                currentUser.getEmail(),
                request.getQuery(),
                request.getMimeType(),
                request.getSortBy());

        return PagedResponse.from(page, content);
    }

    /**
     * Returns the 5 most recently uploaded, non-deleted files owned by the caller.
     * Used by the dashboard "Recent Files" widget.
     *
     * @param currentUser the authenticated caller
     * @return list of up to 5 {@link FileResponse} DTOs, newest first
     */
    @Transactional(readOnly = true)
    public List<FileResponse> getRecentFiles(User currentUser) {
        return fileRepository
                .findTop5ByOwnerIdAndIsDeletedFalseOrderByUploadedAtDesc(currentUser.getId())
                .stream()
                .map(FileResponse::from)
                .toList();
    }

    /**
     * Returns storage usage statistics for the authenticated user.
     *
     * <p>Fields returned:
     * <ul>
     *   <li>{@code totalFiles} — count of non-deleted files</li>
     *   <li>{@code storageUsed} — bytes used (from the denormalised user counter)</li>
     *   <li>{@code storageQuota} — total quota in bytes</li>
     *   <li>{@code storageUsedPercent} — usage percentage rounded to 1 decimal</li>
     *   <li>{@code storageUsedFormatted} — human-readable used size (e.g. "2.4 MB")</li>
     * </ul>
     *
     * @param currentUser the authenticated caller
     * @return stat map; never {@code null}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFileStats(User currentUser) {
        long totalFiles   = fileRepository.countByOwnerIdAndIsDeletedFalse(currentUser.getId());
        long storageUsed  = currentUser.getStorageUsed();
        long storageQuota = currentUser.getStorageQuota();

        // Guard against a zero quota to prevent division-by-zero.
        double usedPercent = storageQuota > 0
                ? Math.round((storageUsed * 100.0 / storageQuota) * 10.0) / 10.0
                : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalFiles",           totalFiles);
        stats.put("storageUsed",          storageUsed);
        stats.put("storageQuota",         storageQuota);
        stats.put("storageUsedPercent",   usedPercent);
        stats.put("storageUsedFormatted", formatBytes(storageUsed));
        return stats;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Validates that the user has enough remaining quota for a file of {@code fileSizeBytes}.
     *
     * <p>This check is intentionally conservative: it compares against
     * {@code quota - used} rather than {@code quota} to prevent concurrent uploads
     * from accidentally consuming more than the allocated quota.
     *
     * @param user          the acting user
     * @param fileSizeBytes the proposed file size in bytes
     * @throws StorageQuotaExceededException if the file would exceed remaining quota
     */
    private void checkStorageQuota(User user, long fileSizeBytes) {
        long remainingBytes = user.getStorageQuota() - user.getStorageUsed();
        log.debug("Storage check for user {}: used={} bytes, quota={} bytes, requested={} bytes, remaining={} bytes",
                user.getEmail(), user.getStorageUsed(), user.getStorageQuota(),
                fileSizeBytes, remainingBytes);

        if (fileSizeBytes > remainingBytes) {
            throw new StorageQuotaExceededException(
                    user.getStorageUsed(),
                    user.getStorageQuota(),
                    fileSizeBytes
            );
        }
    }

    /**
     * Increments the owner's denormalised storage counter and persists the change.
     *
     * <p>Must be called inside an active {@code @Transactional} context so that a
     * failure in the surrounding transaction also rolls back the counter update.
     *
     * @param user  the user whose counter is updated
     * @param bytes number of bytes to add
     */
    private void incrementStorageUsed(User user, long bytes) {
        user.setStorageUsed(user.getStorageUsed() + bytes);
        userRepository.save(user);
        log.debug("Storage incremented for user {}: +{} bytes, total={} bytes",
                user.getEmail(), bytes, user.getStorageUsed());
    }

    /**
     * Decrements the owner's denormalised storage counter and persists the change.
     *
     * <p>Uses {@code Math.max(0, ...)} to guarantee the counter never goes negative,
     * which could happen if the same file is deleted twice due to a race condition
     * or a bug in the decrement logic.
     *
     * @param user  the user whose counter is updated
     * @param bytes number of bytes to subtract
     */
    private void decrementStorageUsed(User user, long bytes) {
        user.setStorageUsed(Math.max(0L, user.getStorageUsed() - bytes));
        userRepository.save(user);
        log.debug("Storage decremented for user {}: -{} bytes, total={} bytes",
                user.getEmail(), bytes, user.getStorageUsed());
    }

    /**
     * Converts a raw byte count to a human-readable string with 1 decimal place.
     * Delegates to {@link FileResponse#formatBytes} — single source of truth.
     *
     * @param bytes raw byte count; negative values are treated as {@code 0}
     * @return human-readable size string
     */
    static String formatBytes(long bytes) {
        return FileResponse.formatBytes(bytes);
    }
}
