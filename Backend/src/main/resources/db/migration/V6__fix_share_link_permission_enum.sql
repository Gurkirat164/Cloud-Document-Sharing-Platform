-- =============================================================
--  CloudVault — Normalize share_links permission enum
--  Flyway: V6__fix_share_link_permission_enum.sql
-- =============================================================

UPDATE share_links
SET permission = 'VIEW'
WHERE permission = 'DOWNLOAD';

ALTER TABLE share_links
    MODIFY COLUMN permission ENUM('VIEW','EDIT') NOT NULL;