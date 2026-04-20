package com.cloudvault.file.service;

// PREREQUISITE: S3 bucket must have versioning enabled —
// AWS Console → S3 → cloudvault-dev-files → Properties → Bucket Versioning → Enable

import com.cloudvault.file.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * Thin wrapper around AWS SDK v2 S3 operations:
 * - Presigned PUT URL generation for direct browser → S3 uploads
 * - Presigned GET URL generation for secure downloads
 * - Hard delete of S3 objects (used during cleanup jobs or admin operations)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry-minutes:15}")
    private long presignedUrlExpiryMinutes;

    /**
     * Generates a presigned PUT URL that the browser uses to upload directly to S3.
     *
     * @param s3Key       the full object key, e.g. "users/{userUuid}/{fileUuid}-filename.pdf"
     * @param contentType the MIME type declared by the client, e.g. "application/pdf"
     * @return PresignedUrlResponse containing the URL, the confirmed key, and TTL in seconds
     */
    public PresignedUrlResponse generatePresignedPutUrl(String s3Key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        log.debug("Generated presigned PUT URL for key: {}", s3Key);

        return new PresignedUrlResponse(
                presignedRequest.url().toString(),
                s3Key,
                presignedUrlExpiryMinutes * 60
        );
    }

    /**
     * Generates a presigned GET URL for time-limited download access.
     *
     * @param s3Key the full object key
     * @return presigned URL string
     */
    public String generatePresignedGetUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        log.debug("Generated presigned GET URL for key: {}", s3Key);
        return presignedRequest.url().toString();
    }

    /**
     * Generates a presigned GET URL for time-limited download access with a custom filename mapping.
     */
    public String generatePresignedGetUrl(String s3Key, String originalFileName, long durationMinutes) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .responseContentDisposition("attachment; filename=\"" + originalFileName + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(durationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        log.debug("Generated presigned GET URL for key: {} with custom filename", s3Key);
        return presignedRequest.url().toString();
    }

    /**
     * Hard-deletes an object from S3. Should only be called by cleanup jobs or admin
     * operations — not triggered directly from a soft-delete API call.
     *
     * @param s3Key the full object key to delete
     */
    public void deleteObject(String s3Key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteRequest);
        log.info("Deleted S3 object: {}", s3Key);
    }

    public String getBucketName() {
        return bucketName;
    }

    // ── Versioning methods ────────────────────────────────────────────────────
    // PREREQUISITE: S3 bucket must have versioning enabled.
    // AWS Console → S3 → cloudvault-dev-files → Properties → Bucket Versioning → Enable

    /**
     * Lists all non-deleted object versions for a given S3 key, sorted newest-first.
     *
     * <p>Delete markers are filtered out — this method returns only actual content
     * versions, not the markers that represent logical deletions.
     *
     * @param s3Key the full S3 object key to list versions for
     * @return list of {@link ObjectVersion}, sorted by last-modified descending;
     *         empty list if the key does not exist or versioning is not enabled
     */
    public List<ObjectVersion> getObjectVersions(String s3Key) {
        log.debug("Listing versions for S3 key: {}", s3Key);

        ListObjectVersionsRequest request = ListObjectVersionsRequest.builder()
                .bucket(bucketName)
                .prefix(s3Key)   // prefix match — only returns versions for this exact key
                .build();

        ListObjectVersionsResponse response = s3Client.listObjectVersions(request);

        return response.versions().stream()
                // Filter to exact key match (prefix may match longer keys)
                .filter(v -> v.key().equals(s3Key))
                // Filter out delete markers by only including ObjectVersion entries
                .sorted(Comparator.comparing(ObjectVersion::lastModified).reversed())
                .toList();
    }

    /**
     * Copies a specific version of an S3 object to a destination key.
     *
     * <p>When bucket versioning is enabled, the copy operation creates a new
     * version at {@code destinationKey} with a brand-new {@code versionId}.
     * The source version is not modified or deleted.
     *
     * <p>Used by the version restore flow: "restoring" a version means copying
     * old content to the same key, creating a new current S3 version.
     *
     * @param s3Key          source object key
     * @param s3VersionId    the specific S3 version ID to copy
     * @param destinationKey target object key (usually the same as {@code s3Key}
     *                       for in-place restore)
     * @return the new S3 version ID assigned to the copied object at {@code destinationKey}
     */
    public String copyVersion(String s3Key, String s3VersionId, String destinationKey) {
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(s3Key)
                .sourceVersionId(s3VersionId)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .build();

        CopyObjectResponse copyResponse = s3Client.copyObject(copyRequest);

        String newVersionId = copyResponse.versionId();
        log.info("Copied S3 version {} of key {} to {}", s3VersionId, s3Key, destinationKey);
        return newVersionId;
    }

    /**
     * Permanently deletes a specific version of an S3 object.
     *
     * <p>Unlike a regular delete (which creates a delete marker when versioning is
     * enabled), specifying a {@code versionId} bypasses the marker mechanism and
     * removes the bytes from S3 permanently. This operation cannot be undone.
     *
     * @param s3Key       the S3 object key
     * @param s3VersionId the specific version ID to permanently remove
     */
    public void deleteVersion(String s3Key, String s3VersionId) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .versionId(s3VersionId)
                .build();

        s3Client.deleteObject(deleteRequest);
        log.info("Permanently deleted S3 version {} of key {}", s3VersionId, s3Key);
    }

    /**
     * Returns the S3 version ID of the latest (current) version of an object.
     *
     * <p>Uses {@code HeadObject} to fetch metadata without downloading the object.
     * The version ID is returned in the response metadata when bucket versioning
     * is enabled.
     *
     * @param s3Key the S3 object key
     * @return the version ID of the latest version, or {@code null} if the object
     *         does not exist or versioning is not enabled on the bucket
     */
    public String getLatestVersionId(String s3Key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            return headResponse.versionId();
        } catch (NoSuchKeyException ex) {
            log.debug("HeadObject: key does not exist yet — {}", s3Key);
            return null;
        } catch (Exception ex) {
            log.warn("HeadObject failed for key {}: {}", s3Key, ex.getMessage());
            return null;
        }
    }
}

