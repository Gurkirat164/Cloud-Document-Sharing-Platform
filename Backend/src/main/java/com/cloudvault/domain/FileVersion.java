package com.cloudvault.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity representing a single version of a {@link File}.
 *
 * <p>A new {@link FileVersion} record is created each time:
 * <ul>
 *   <li>A file is uploaded for the first time (version 1).</li>
 *   <li>A file is re-uploaded to the same S3 key (version N+1).</li>
 *   <li>A previous version is restored — restoration copies the old S3
 *       object to a new S3 version, which becomes the new current version.</li>
 * </ul>
 *
 * <p>Only one version per file may have {@code isCurrentVersion = true} at
 * any point in time. This invariant is enforced by {@code FileVersionService}.
 *
 * <p>PREREQUISITE: S3 bucket versioning must be enabled — each PUT to S3
 * returns a unique {@code versionId} which is stored here.
 */
@Entity
@Table(
        name = "file_versions",
        indexes = {
                @Index(name = "idx_fv_file_id",        columnList = "file_id"),
                @Index(name = "idx_fv_version_number", columnList = "file_id, version_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * The parent file this version belongs to.
     * On file deletion ({@code is_deleted = true}) the parent still exists;
     * the version record is preserved so the history is never lost.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    /**
     * Sequential 1-based version counter, scoped per file.
     * Monotonically increasing — never reused even after a version is deleted.
     */
    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    /** AWS S3 version ID returned by the service when the object was PUT. */
    @Column(name = "s3_version_id", nullable = false, length = 512)
    private String s3VersionId;

    /** Full S3 object key for this version (usually the same as the file key). */
    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    /** Filename as supplied by the user at the time of this upload. */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** File size in bytes for this specific version. */
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /** MIME type for this version; may differ across versions if the file was replaced. */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** SHA-256 hex digest of the content at this version; {@code null} if not provided. */
    @Column(name = "checksum", length = 64)
    private String checksum;

    /** The user who uploaded or triggered the creation of this version. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    /**
     * Whether this is the currently active version of the file.
     * All other versions for the same file have this set to {@code false}.
     */
    @Column(name = "is_current_version", nullable = false)
    @Builder.Default
    private boolean isCurrentVersion = true;

    /**
     * If this version was created by restoring a previous version, this field
     * holds the version number that was restored. {@code null} for regular uploads.
     */
    @Column(name = "restored_from_version")
    private Integer restoredFromVersion;

    /** Timestamp when this version record was created. Set in {@link #prePersist()}. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }
}
