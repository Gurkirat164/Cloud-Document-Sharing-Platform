package com.cloudvault.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Sent by the frontend after a successful direct S3 PUT upload.
 * Contains enough metadata for the backend to persist the file record.
 */
public record FileMetadataRequest(

        @NotBlank(message = "Original filename is required")
        String originalName,

        @NotBlank(message = "S3 key is required")
        String s3Key,

        @NotBlank(message = "MIME type is required")
        String mimeType,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        Long sizeBytes,

        /** SHA-256 hex digest of the file content — optional but recommended. */
        String checksum
) {}
