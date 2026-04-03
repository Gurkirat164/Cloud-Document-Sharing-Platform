-- =============================================================
--  CloudVault — MySQL 8 Database Schema
--  File    : Schema.sql
--  Engine  : InnoDB | Charset: utf8mb4
--  Tables  : users, refresh_tokens, files, file_permissions,
--            share_links, activity_logs (created in FK-safe order)
-- =============================================================

CREATE DATABASE IF NOT EXISTS cloudvault
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cloudvault;

-- =============================================================
-- 1. USERS
--    • Dual-ID: id (internal JOINs) + uuid (public API exposure)
--    • storage_used is denormalized for O(1) quota checks;
--      updated by application on every upload / soft-delete
--    • password_hash stores bcrypt / Argon2 — never plaintext
--    • role ENUM enforced at DB level; Spring Security maps to authorities
-- =============================================================
CREATE TABLE users (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    uuid           CHAR(36)         NOT NULL,
    email          VARCHAR(255)     NOT NULL,
    password_hash  VARCHAR(255)     NOT NULL,
    full_name      VARCHAR(100)     NOT NULL,
    role           ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    storage_used   BIGINT           NOT NULL DEFAULT 0               COMMENT 'Current usage in bytes',
    storage_quota  BIGINT           NOT NULL DEFAULT 5368709120      COMMENT 'Default quota: 5 GB',
    is_active      BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP        NOT NULL DEFAULT NOW() ON UPDATE NOW(),

    CONSTRAINT pk_users          PRIMARY KEY  (id),
    CONSTRAINT uq_users_uuid     UNIQUE       (uuid),
    CONSTRAINT uq_users_email    UNIQUE       (email),
    CONSTRAINT chk_storage_used  CHECK        (storage_used >= 0),
    CONSTRAINT chk_storage_quota CHECK        (storage_quota > 0)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Platform user accounts';


-- =============================================================
-- 2. REFRESH_TOKENS
--    • Stores JWT refresh tokens tied to a user session
--    • ON DELETE CASCADE: all tokens purged when user is deleted
--    • revoked_at NULL = token is still valid (not yet revoked)
--    • Enables: multi-device logout, forced session invalidation
-- =============================================================
CREATE TABLE refresh_tokens (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    token       VARCHAR(512)     NOT NULL,
    user_id     BIGINT UNSIGNED  NOT NULL,
    expires_at  TIMESTAMP        NOT NULL,
    revoked_at  TIMESTAMP        NULL     COMMENT 'NULL = active token',
    created_at  TIMESTAMP        NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_tokens        PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token         UNIQUE      (token(255)),   -- prefix index on VARCHAR(512)
    CONSTRAINT fk_rt_user               FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE               -- purge sessions on account deletion
        ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='JWT refresh tokens for persistent login sessions';


-- =============================================================
-- 3. FILES
--    • s3_key is the canonical S3 object path — globally unique
--    • checksum (SHA-256 hex) for content integrity on download
--    • Soft delete (is_deleted + deleted_at):
--        - Allows file restore without data loss
--        - Keeps audit_logs references intact
--        - Async cleanup job sweeps S3 for rows deleted > N days
-- =============================================================
CREATE TABLE files (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    uuid           CHAR(36)         NOT NULL,
    owner_id       BIGINT UNSIGNED  NOT NULL,
    original_name  VARCHAR(255)     NOT NULL COMMENT 'Filename as uploaded by the user',
    s3_key         VARCHAR(512)     NOT NULL COMMENT 'Full S3 object key',
    s3_bucket      VARCHAR(100)     NOT NULL COMMENT 'S3 bucket name (≤100 chars)',
    mime_type      VARCHAR(100)     NULL,
    size_bytes     BIGINT UNSIGNED  NOT NULL,
    checksum       VARCHAR(64)      NULL     COMMENT 'SHA-256 hex digest',
    is_deleted     BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP        NULL,
    uploaded_at    TIMESTAMP        NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_files          PRIMARY KEY (id),
    CONSTRAINT uq_files_uuid     UNIQUE      (uuid),
    CONSTRAINT uq_files_s3_key   UNIQUE      (s3_key(255)),         -- prefix index on VARCHAR(512)
    CONSTRAINT fk_files_owner    FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT                  -- prevent orphaned S3 objects
        ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Metadata for files stored in AWS S3';


-- =============================================================
-- 4. FILE_PERMISSIONS
--    • Named user-to-user sharing (authenticated, not anonymous)
--    • UNIQUE (file_id, grantee_id): one record per (file, user) pair;
--      upgrade VIEW → EDIT by updating the row, not inserting a new one
--    • granted_by track who created the share for audit purposes
--    • expires_at NULL = permission never expires
--    • Separation from share_links:
--        file_permissions = named users, trackable, EDIT-capable
--        share_links      = anonymous tokens, VIEW/DOWNLOAD only
-- =============================================================
CREATE TABLE file_permissions (
    id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    file_id      BIGINT UNSIGNED  NOT NULL,
    grantee_id   BIGINT UNSIGNED  NOT NULL,
    permission   ENUM('VIEW','EDIT') NOT NULL,
    granted_by   BIGINT UNSIGNED  NOT NULL,
    expires_at   TIMESTAMP        NULL     COMMENT 'NULL = never expires',
    is_active    BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP        NOT NULL DEFAULT NOW() ON UPDATE NOW(),

    CONSTRAINT pk_file_permissions  PRIMARY KEY (id),
    CONSTRAINT uq_file_grantee      UNIQUE      (file_id, grantee_id),
    CONSTRAINT fk_fp_file           FOREIGN KEY (file_id)
        REFERENCES files(id)
        ON DELETE CASCADE            -- revoke all permissions when file is deleted
        ON UPDATE CASCADE,
    CONSTRAINT fk_fp_grantee        FOREIGN KEY (grantee_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_fp_granted_by     FOREIGN KEY (granted_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Authenticated user-to-user file sharing permissions';


-- =============================================================
-- 5. SHARE_LINKS
--    • Anonymous token-based public sharing (anyone with the URL)
--    • password_hash: optional extra layer — link requires a password
--    • max_uses NULL = unlimited; use_count tracks actual accesses
--    • When use_count >= max_uses the link is effectively exhausted
-- =============================================================
CREATE TABLE share_links (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    token          VARCHAR(128)     NOT NULL,
    file_id        BIGINT UNSIGNED  NOT NULL,
    created_by     BIGINT UNSIGNED  NOT NULL,
    permission     ENUM('VIEW','DOWNLOAD') NOT NULL,
    password_hash  VARCHAR(255)     NULL     COMMENT 'Optional: bcrypt hash for password-protected links',
    max_uses       INT              NULL     COMMENT 'NULL = unlimited',
    use_count      INT              NOT NULL DEFAULT 0,
    expires_at     TIMESTAMP        NULL     COMMENT 'NULL = never expires',
    is_active      BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP        NOT NULL DEFAULT NOW() ON UPDATE NOW(),

    CONSTRAINT pk_share_links       PRIMARY KEY (id),
    CONSTRAINT uq_share_token       UNIQUE      (token),
    CONSTRAINT fk_sl_file           FOREIGN KEY (file_id)
        REFERENCES files(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_sl_created_by     FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_use_count        CHECK (use_count >= 0),
    CONSTRAINT chk_max_uses         CHECK (max_uses IS NULL OR max_uses > 0)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Token-based anonymous public share links';


-- =============================================================
-- 6. ACTIVITY_LOGS
--    • Append-only audit trail — application must NEVER UPDATE or
--      DELETE rows from this table
--    • user_id NULL: allows logging anonymous share_link access
--    • file_id NULL: allows logging non-file events (LOGIN, LOGOUT)
--    • ON DELETE SET NULL on all FKs: logs survive user/file deletion
--    • metadata JSON: event-specific context (old/new permission,
--      failure reason, presigned URL expiry, etc.)
-- =============================================================
CREATE TABLE activity_logs (
    id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED  NULL,
    file_id        BIGINT UNSIGNED  NULL,
    event_type     ENUM(
                       'UPLOAD',
                       'DOWNLOAD',
                       'VIEW',
                       'DELETE',
                       'RESTORE',
                       'SHARE_GRANT',
                       'SHARE_REVOKE',
                       'SHARE_LINK_CREATE',
                       'SHARE_LINK_ACCESS',
                       'SHARE_LINK_EXPIRE',
                       'LOGIN',
                       'LOGOUT'
                   ) NOT NULL,
    ip_address     VARCHAR(45)      NULL COMMENT 'IPv4 or IPv6',
    user_agent     VARCHAR(512)     NULL,
    share_link_id  BIGINT UNSIGNED  NULL,
    metadata       JSON             NULL COMMENT 'Event-specific extra context',
    created_at     TIMESTAMP        NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_activity_logs    PRIMARY KEY (id),
    CONSTRAINT fk_al_user          FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL          -- logs outlive user accounts
        ON UPDATE CASCADE,
    CONSTRAINT fk_al_file          FOREIGN KEY (file_id)
        REFERENCES files(id)
        ON DELETE SET NULL          -- logs outlive files
        ON UPDATE CASCADE,
    CONSTRAINT fk_al_share_link    FOREIGN KEY (share_link_id)
        REFERENCES share_links(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Immutable audit trail — append only, no UPDATE or DELETE';
