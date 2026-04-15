package com.cloudvault.file.dto;

/**
 * Response returned when the client requests a presigned PUT URL.
 * The frontend uses {@code presignedUrl} to PUT the file directly to S3,
 * then sends {@code s3Key} back in the metadata save request.
 */
public record PresignedUrlResponse(
        String presignedUrl,
        String s3Key,
        long expiresInSeconds
) {}
