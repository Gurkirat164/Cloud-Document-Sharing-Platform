package com.cloudvault.file.service;

import com.cloudvault.file.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;

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
}
