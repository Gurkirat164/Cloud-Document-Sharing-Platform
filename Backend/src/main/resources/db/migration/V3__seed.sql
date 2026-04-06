-- =============================================================
--  CloudVault — Dev Bootstrap / Seed Data
--  Flyway: V3__seed.sql
--  Purpose : Insert realistic dev data + reference queries
--  WARNING : Replace placeholder hashes before any real testing
--  Run     : AFTER V1__schema.sql and V2__indexes.sql
-- =============================================================

-- =============================================================
-- USERS (3 rows)
--   password_hash: bcrypt of 'Password@123', cost factor 10
--   Replace $2a$10$PLACEHOLDER_HASH_FOR_DEV_ONLY with real hashes
-- =============================================================
INSERT INTO users (uuid, email, password_hash, full_name, role, storage_quota) VALUES
    (UUID(), 'admin@cloudvault.dev',
     '$2a$10$PLACEHOLDER_HASH_FOR_DEV_ONLY',
     'CloudVault Admin', 'ADMIN', 107374182400),     -- 100 GB quota for admin

    (UUID(), 'alice@cloudvault.dev',
     '$2a$10$PLACEHOLDER_HASH_FOR_DEV_ONLY',
     'Alice Wonderland', 'USER', 5368709120),         -- 5 GB default quota

    (UUID(), 'bob@cloudvault.dev',
     '$2a$10$PLACEHOLDER_HASH_FOR_DEV_ONLY',
     'Bob Builder', 'USER', 5368709120);


-- =============================================================
-- FILES (2 rows — both owned by alice, user id = 2)
-- =============================================================
INSERT INTO files (uuid, owner_id, original_name, s3_key, s3_bucket, mime_type, size_bytes, checksum) VALUES
    (UUID(), 2,
     'Project_Proposal.pdf',
     'users/alice/documents/project_proposal_2024.pdf',
     'cloudvault-prod-files',
     'application/pdf',
     2097152,                                         -- 2 MB
     'a3f1c29e4b87d6e501f234c891ab7d2e4f6789012345678901234567890abcdef'),

    (UUID(), 2,
     'Profile_Photo.png',
     'users/alice/images/profile_photo_hd.png',
     'cloudvault-prod-files',
     'image/png',
     524288,                                          -- 512 KB
     'b7e2d14f3a96c5d812e345f902bc8e1f5a7890123456789012345678901234ab');

-- Update alice's storage_used to reflect the 2 files above (2 MB + 512 KB)
UPDATE users SET storage_used = 2621440 WHERE id = 2;


-- =============================================================
-- FILE_PERMISSIONS (1 row)
--   Bob gets VIEW access to alice's first file (Project_Proposal.pdf)
--   Granted by alice (user id = 2), expires in 30 days
-- =============================================================
INSERT INTO file_permissions (file_id, grantee_id, permission, granted_by, expires_at) VALUES
    (1, 3, 'VIEW', 2, DATE_ADD(NOW(), INTERVAL 30 DAY));


-- =============================================================
-- SHARE_LINKS (1 row)
--   Public download link for alice's second file (Profile_Photo.png)
--   Max 10 uses, expires in 7 days, no password protection
-- =============================================================
INSERT INTO share_links (token, file_id, created_by, permission, max_uses, expires_at) VALUES
    (UUID(), 2, 2, 'DOWNLOAD', 10, DATE_ADD(NOW(), INTERVAL 7 DAY));


-- =============================================================
-- REFERENCE QUERIES (commented out — uncomment to run)
-- =============================================================

-- 1. Storage utilisation per user
-- SELECT u.email, u.full_name,
--        ROUND(u.storage_used  / 1073741824, 3) AS used_gb,
--        ROUND(u.storage_quota / 1073741824, 0) AS quota_gb,
--        ROUND(u.storage_used * 100.0 / u.storage_quota, 1) AS pct_used
-- FROM users u
-- ORDER BY pct_used DESC;


-- 2. All files shared with a specific user (replace ? with grantee user id)
-- SELECT f.uuid, f.original_name, f.mime_type, fp.permission, fp.expires_at,
--        owner.full_name AS shared_by
-- FROM file_permissions fp
-- JOIN files f        ON f.id = fp.file_id
-- JOIN users owner    ON owner.id = f.owner_id
-- WHERE fp.grantee_id = ?
--   AND fp.is_active = TRUE
--   AND (fp.expires_at IS NULL OR fp.expires_at > NOW());


-- 3. All expired or exhausted share links (candidates for deactivation)
-- SELECT sl.token, sl.permission, sl.use_count, sl.max_uses,
--        sl.expires_at, f.original_name, u.email AS created_by
-- FROM share_links sl
-- JOIN files f ON f.id = sl.file_id
-- JOIN users u ON u.id = sl.created_by
-- WHERE sl.is_active = TRUE
--   AND (
--       (sl.expires_at IS NOT NULL AND sl.expires_at <= NOW())
--    OR (sl.max_uses   IS NOT NULL AND sl.use_count >= sl.max_uses)
--   );


-- 4. Active sessions (non-revoked, non-expired refresh tokens) per user
-- SELECT u.email, u.full_name, COUNT(rt.id) AS active_sessions,
--        MAX(rt.created_at) AS last_login
-- FROM refresh_tokens rt
-- JOIN users u ON u.id = rt.user_id
-- WHERE rt.revoked_at IS NULL
--   AND rt.expires_at > NOW()
-- GROUP BY u.id, u.email, u.full_name
-- ORDER BY active_sessions DESC;


-- 5. Download count per file (all time)
-- SELECT f.original_name, f.uuid,
--        COUNT(al.id) AS total_downloads,
--        MAX(al.created_at) AS last_downloaded_at
-- FROM activity_logs al
-- JOIN files f ON f.id = al.file_id
-- WHERE al.event_type = 'DOWNLOAD'
-- GROUP BY f.id, f.original_name, f.uuid
-- ORDER BY total_downloads DESC;


-- 6. Full audit trail for a specific file, paginated (replace ? with file id)
-- SELECT al.event_type, al.ip_address, u.email AS actor,
--        al.metadata, al.created_at
-- FROM activity_logs al
-- LEFT JOIN users u ON u.id = al.user_id
-- WHERE al.file_id = ?
-- ORDER BY al.created_at DESC
-- LIMIT 50 OFFSET 0;


-- 7. Admin view: all activity in the last 24 hours
-- SELECT al.event_type, u.email AS actor, f.original_name AS file,
--        al.ip_address, al.created_at
-- FROM activity_logs al
-- LEFT JOIN users u ON u.id = al.user_id
-- LEFT JOIN files f ON f.id = al.file_id
-- WHERE al.created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
-- ORDER BY al.created_at DESC;
