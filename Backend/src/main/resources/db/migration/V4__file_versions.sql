-- =============================================================
--  CloudVault — Flyway Migration V4
--  Adds file_versions table and current_s3_version_id to files
--
--  PREREQUISITE: S3 bucket versioning must be enabled on the
--  cloudvault-dev-files bucket before this feature is used.
--  AWS Console → S3 → cloudvault-dev-files → Properties →
--  Bucket Versioning → Enable
-- =============================================================

-- ── 1. Add current_s3_version_id to files table ───────────────────────────
ALTER TABLE files
    ADD COLUMN current_s3_version_id VARCHAR(512) NULL
        COMMENT 'S3 version ID of the current active version';

-- ── 2. Create file_versions table ────────────────────────────────────────────
CREATE TABLE file_versions (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    file_id               BIGINT UNSIGNED NOT NULL,
    version_number        INT             NOT NULL,
    s3_version_id         VARCHAR(512)    NOT NULL,
    s3_key                VARCHAR(512)    NOT NULL,
    original_name         VARCHAR(255)    NOT NULL,
    size_bytes            BIGINT UNSIGNED NOT NULL,
    mime_type             VARCHAR(100)    NULL,
    checksum              VARCHAR(64)     NULL,
    uploaded_by           BIGINT UNSIGNED NOT NULL,
    is_current_version    BOOLEAN         NOT NULL DEFAULT TRUE,
    restored_from_version INT             NULL,
    created_at            TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_file_versions  PRIMARY KEY (id),
    CONSTRAINT fk_fv_file        FOREIGN KEY (file_id)
        REFERENCES files(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_fv_uploaded_by FOREIGN KEY (uploaded_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    INDEX idx_fv_file_id      (file_id),
    INDEX idx_fv_file_version (file_id, version_number)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Per-file version history, linked to S3 object versions';
