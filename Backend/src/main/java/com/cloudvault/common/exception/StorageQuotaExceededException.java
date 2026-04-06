package com.cloudvault.common.exception;

// Thrown when a file upload would exceed the user's allocated storage quota.
public class StorageQuotaExceededException extends RuntimeException {
    public StorageQuotaExceededException(String message) {
        super(message);
    }
}
