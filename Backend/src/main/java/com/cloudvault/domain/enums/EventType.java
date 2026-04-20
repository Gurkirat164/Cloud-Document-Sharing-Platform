package com.cloudvault.domain.enums;

/**
 * Audit event types recorded in activity_logs.
 *
 * <p>Must remain consistent with the {@code event_type} ENUM defined in
 * {@code V1__schema.sql}. FILE_RESTORE maps to the DB value 'RESTORE'.
 */
public enum EventType {
    FILE_UPLOAD,
    FILE_DOWNLOAD,
    FILE_DELETE,
    FILE_RENAME,
    FILE_SHARE,
    FILE_RESTORE,
    PERMISSION_GRANT,
    PERMISSION_REVOKE,
    USER_LOGIN,
    USER_LOGOUT,
    USER_REGISTER
}
