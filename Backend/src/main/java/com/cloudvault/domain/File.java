package com.cloudvault.domain;

import com.cloudvault.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to the `files` table.
 * Represents file metadata stored in MySQL; actual bytes live in AWS S3.
 */
@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false, columnDefinition = "CHAR(36)", unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    /**
     * The S3 version ID of the current active version.
     * Populated after each upload when bucket versioning is enabled.
     * {@code null} when the bucket does not have versioning enabled.
     */
    @Column(name = "current_s3_version_id", length = 512)
    private String currentS3VersionId;

    @PrePersist
    private void prePersist() {
        this.uuid = UUID.randomUUID().toString();
        this.uploadedAt = Instant.now();
    }
}
