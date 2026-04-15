package com.cloudvault.file.dto;

import com.cloudvault.domain.File;

import java.time.Instant;

/**
 * Public-facing file representation returned by the API.
 * Exposes only safe fields — never leaks internal DB ids or S3 internals.
 */
public record FileResponse(
        String uuid,
        String originalName,
        String mimeType,
        Long sizeBytes,
        String s3Key,
        String ownerEmail,
        Instant uploadedAt
) {
    public static FileResponse from(File file) {
        return new FileResponse(
                file.getUuid(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getSizeBytes(),
                file.getS3Key(),
                file.getOwner().getEmail(),
                file.getUploadedAt()
        );
    }
}
