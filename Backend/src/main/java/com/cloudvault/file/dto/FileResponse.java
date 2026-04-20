package com.cloudvault.file.dto;

import com.cloudvault.domain.File;

import java.time.Instant;

/**
 * Public-facing file representation returned by the API.
 *
 * <p>Exposes only safe fields — internal DB ids and raw S3 paths that could
 * expose bucket structure are deliberately excluded.
 *
 * <p>The {@link #from(File)} factory is NPE-safe for all nullable associations.
 */
public record FileResponse(
        String uuid,
        String originalName,
        String mimeType,
        Long sizeBytes,
        String sizeFormatted,
        String s3Key,
        String checksum,
        boolean isDeleted,
        Instant uploadedAt,
        String ownerEmail,
        String ownerUuid
) {

    /**
     * Maps a {@link File} JPA entity to a {@link FileResponse} DTO.
     *
     * @param file the entity to map; must not be {@code null}
     * @return an immutable DTO safe for JSON serialisation
     */
    public static FileResponse from(File file) {
        return new FileResponse(
                file.getUuid(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getSizeBytes(),
                formatBytes(file.getSizeBytes()),
                file.getS3Key(),
                file.getChecksum(),
                Boolean.TRUE.equals(file.getIsDeleted()),
                file.getUploadedAt(),
                file.getOwner() != null ? file.getOwner().getEmail() : null,
                file.getOwner() != null ? file.getOwner().getUuid()  : null
        );
    }

    /**
     * Converts a raw byte count to a human-readable string with 1 decimal place.
     *
     * <ul>
     *   <li>{@code < 1,024}            → {@code "512 B"}</li>
     *   <li>{@code < 1,048,576}        → {@code "3.5 KB"}</li>
     *   <li>{@code < 1,073,741,824}    → {@code "2.4 MB"}</li>
     *   <li>otherwise                  → {@code "1.1 GB"}</li>
     * </ul>
     *
     * @param bytes raw byte count; negative values are treated as {@code 0}
     * @return human-readable size string
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) bytes = 0;
        if (bytes < 1_024L)              return bytes + " B";
        if (bytes < 1_048_576L)          return String.format("%.1f KB", bytes / 1_024.0);
        if (bytes < 1_073_741_824L)      return String.format("%.1f MB", bytes / 1_048_576.0);
        return                                  String.format("%.1f GB", bytes / 1_073_741_824.0);
    }
}
