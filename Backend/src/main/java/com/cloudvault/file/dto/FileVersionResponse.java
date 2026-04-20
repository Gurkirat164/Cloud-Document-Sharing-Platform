package com.cloudvault.file.dto;

import com.cloudvault.domain.FileVersion;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Public-facing representation of a single {@link FileVersion}.
 *
 * <p>Returned by version history and restore endpoints. Internal DB IDs
 * are excluded; the parent file is identified by its public UUID.
 */
@Getter
@Builder
public class FileVersionResponse {

    private final Long    id;
    private final String  fileUuid;
    private final int     versionNumber;
    private final String  s3VersionId;
    private final String  originalName;
    private final long    sizeBytes;
    private final String  sizeFormatted;
    private final String  mimeType;
    private final String  checksum;
    private final String  uploadedByEmail;
    private final String  uploadedByName;
    private final boolean isCurrentVersion;

    /** Non-null only when this version was created by restoring an older version. */
    private final Integer restoredFromVersion;

    private final Instant createdAt;

    /**
     * Maps a {@link FileVersion} entity to a {@link FileVersionResponse} DTO.
     *
     * <p>All nullable associations ({@code file}, {@code uploadedBy}) are
     * handled defensively to prevent NPE in edge cases.
     *
     * @param fv the entity to map; must not be {@code null}
     * @return immutable DTO safe for JSON serialisation
     */
    public static FileVersionResponse from(FileVersion fv) {
        return FileVersionResponse.builder()
                .id(fv.getId())
                .fileUuid(fv.getFile() != null ? fv.getFile().getUuid() : null)
                .versionNumber(fv.getVersionNumber())
                .s3VersionId(fv.getS3VersionId())
                .originalName(fv.getOriginalName())
                .sizeBytes(fv.getSizeBytes())
                .sizeFormatted(FileResponse.formatBytes(fv.getSizeBytes()))
                .mimeType(fv.getMimeType())
                .checksum(fv.getChecksum())
                .uploadedByEmail(fv.getUploadedBy() != null ? fv.getUploadedBy().getEmail()    : null)
                .uploadedByName (fv.getUploadedBy() != null ? fv.getUploadedBy().getFullName() : null)
                .isCurrentVersion(fv.isCurrentVersion())
                .restoredFromVersion(fv.getRestoredFromVersion())
                .createdAt(fv.getCreatedAt())
                .build();
    }
}
