package com.cloudvault.common.exception;

// Thrown when the authenticated user attempts an operation they are not permitted to perform on a resource.
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
