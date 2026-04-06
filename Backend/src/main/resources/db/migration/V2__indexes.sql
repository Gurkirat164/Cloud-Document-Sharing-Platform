-- =============================================================
--  CloudVault — Performance Indexes
--  Flyway: V2__indexes.sql
--  Purpose : Composite indexes tuned for specific query patterns
--  Run     : AFTER V1__schema.sql
-- =============================================================

-- =============================================================
-- FILES
--   Hot query 1: "Give me all non-deleted files for user X"
--     WHERE owner_id = ? AND is_deleted = FALSE
--   Hot query 2: Same as above + sorted by upload date (pagination)
--     WHERE owner_id = ? AND is_deleted = FALSE
--     ORDER BY uploaded_at DESC LIMIT ? OFFSET ?
-- =============================================================
CREATE INDEX idx_files_owner_deleted
    ON files (owner_id, is_deleted);

CREATE INDEX idx_files_owner_deleted_date
    ON files (owner_id, is_deleted, uploaded_at DESC);


-- =============================================================
-- FILE_PERMISSIONS
--   Hot query 1: "Which files can this user access?" (access control on every request)
--     WHERE grantee_id = ? AND is_active = TRUE
--   Hot query 2: Covered by the UNIQUE constraint already created in V1__schema.sql
--     (file_id, grantee_id) — no duplicate index needed, listed for clarity
-- =============================================================
CREATE INDEX idx_fp_grantee_active
    ON file_permissions (grantee_id, is_active);

-- (file_id, grantee_id) UNIQUE index already exists from V1__schema.sql


-- =============================================================
-- SHARE_LINKS
--   Hot path: every public link access does a token lookup + validity check
--     WHERE token = ? AND is_active = TRUE
--       AND (expires_at IS NULL OR expires_at > NOW())
-- =============================================================
CREATE INDEX idx_sl_token_active_expiry
    ON share_links (token, is_active, expires_at);


-- =============================================================
-- ACTIVITY_LOGS
--   Audit query 1: "Show audit trail for file X, paginated"
--     WHERE file_id = ? ORDER BY created_at DESC LIMIT ?
--   Audit query 2: "Show all events of type Y for user X, paginated"
--     WHERE user_id = ? AND event_type = ? ORDER BY created_at DESC LIMIT ?
-- =============================================================
CREATE INDEX idx_al_file_date
    ON activity_logs (file_id, created_at DESC);

CREATE INDEX idx_al_user_event_date
    ON activity_logs (user_id, event_type, created_at DESC);


-- =============================================================
-- REFRESH_TOKENS
--   Active session lookup: "Get all valid tokens for user X"
--     WHERE user_id = ? AND revoked_at IS NULL AND expires_at > NOW()
--   Also used for: "Revoke all sessions for user X" (force logout)
-- =============================================================
CREATE INDEX idx_rt_user_revoked
    ON refresh_tokens (user_id, revoked_at);
