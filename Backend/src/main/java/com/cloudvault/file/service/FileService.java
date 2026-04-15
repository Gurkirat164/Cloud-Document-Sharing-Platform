package com.cloudvault.file.service;

import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.common.exception.StorageQuotaExceededException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.file.dto.FileMetadataRequest;
import com.cloudvault.file.dto.FileResponse;
import com.cloudvault.file.dto.PresignedUrlResponse;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Core business logic for file management:
 * - presigned upload URL generation
 * - saving file metadata after a direct S3 upload
 * - listing user files
 * - soft-deleting files with quota adjustment
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Generates a unique S3 key for the upload and returns a presigned PUT URL.
     * Key format: users/{userUuid}/{newUuid}-{sanitisedFilename}
     *
     * @param filename    original filename from the browser
     * @param contentType MIME type
     * @param user        authenticated user performing the upload
     * @return presigned URL + confirmed S3 key + TTL
     */
    public PresignedUrlResponse getPresignedUploadUrl(String filename, String contentType, User user) {
        String sanitised = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String s3Key = "users/" + user.getUuid() + "/" + UUID.randomUUID() + "-" + sanitised;
        log.debug("Generating presigned PUT URL for user={} key={}", user.getEmail(), s3Key);
        return s3Service.generatePresignedPutUrl(s3Key, contentType);
    }

    /**
     * Persists file metadata to MySQL after the frontend has completed the S3 PUT.
     * Increments the owner's storageUsed counter.
     *
     * @param request metadata sent by the frontend
     * @param user    authenticated uploader
     * @return the saved FileResponse
     */
    @Transactional
    public FileResponse saveFileMetadata(FileMetadataRequest request, User user) {
        // Quota check before saving
        long newTotal = user.getStorageUsed() + request.sizeBytes();
        if (newTotal > user.getStorageQuota()) {
            throw new StorageQuotaExceededException(
                    "Upload would exceed storage quota. Used: " + user.getStorageUsed()
                    + ", quota: " + user.getStorageQuota()
                    + ", file size: " + request.sizeBytes());
        }

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

        // Increment user's storage counter
        user.setStorageUsed(user.getStorageUsed() + request.sizeBytes());
        userRepository.save(user);

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
     * Soft-deletes the DB record and immediately removes the S3 object.
     *
     * <p>Design decisions:
     * <ul>
     *   <li>DB soft-delete happens inside the transaction — it is the source of truth
     *       for file visibility; the row is never shown to users after this point.</li>
     *   <li>S3 deletion happens <em>after</em> the transaction commits. A failed S3 call
     *       is logged as a warning but does NOT roll back the DB record — this prevents
     *       a transient S3 error from leaving the user unable to "delete" a file again.</li>
     *   <li>Any orphaned S3 objects that survive a failed immediate delete will be
     *       cleaned up by a future async job that sweeps rows where
     *       {@code is_deleted = true AND deleted_at < NOW() - INTERVAL N DAYS}.</li>
     * </ul>
     *
     * @param fileUuid public UUID of the file to delete
     * @param user     authenticated user performing the deletion
     */
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

        // 3. Decrement the owner's storage counter (floor at 0)
        long updated = Math.max(0L, user.getStorageUsed() - file.getSizeBytes());
        user.setStorageUsed(updated);
        userRepository.save(user);

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
}
