-- =============================================================
--  CloudVault — Fix activity_logs event_type ENUM mismatch
--  Flyway: V5__fix_event_type_enum.sql
--
--  The Java EventType enum uses descriptive names (FILE_UPLOAD,
--  USER_LOGIN, etc.) but V1__schema.sql defined short names
--  (UPLOAD, LOGIN, etc.). This migration expands the DB ENUM to
--  include all values the application actually attempts to write.
-- =============================================================

ALTER TABLE activity_logs
    MODIFY COLUMN event_type ENUM(
        -- Original short names (kept for backward compat with any existing rows)
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
        'LOGOUT',
        -- New names from the Java EventType enum
        'FILE_UPLOAD',
        'FILE_DOWNLOAD',
        'FILE_DELETE',
        'FILE_RENAME',
        'FILE_SHARE',
        'FILE_RESTORE',
        'PERMISSION_GRANT',
        'PERMISSION_REVOKE',
        'USER_LOGIN',
        'USER_LOGOUT',
        'USER_REGISTER'
    ) NOT NULL;
