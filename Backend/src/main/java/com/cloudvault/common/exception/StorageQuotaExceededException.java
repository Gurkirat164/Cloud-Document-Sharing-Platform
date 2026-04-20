package com.cloudvault.common.exception;

/**
 * Thrown when a file upload would exceed the user's allocated storage quota.
 *
 * <p>Carries structured fields so the exception handler can return a rich
 * 413 PAYLOAD TOO LARGE response with human-readable byte values instead of
 * forcing the client to parse a plain text message string.
 */
public class StorageQuotaExceededException extends RuntimeException {

    private final long usedBytes;
    private final long quotaBytes;
    private final long requestedBytes;

    /**
     * Creates a new {@link StorageQuotaExceededException}.
     *
     * <p>The exception message is formatted as:
     * {@code "Storage quota exceeded. Used: X.X MB, Quota: X.X MB, Requested: X.X MB"}
     *
     * @param usedBytes      how many bytes the user has already consumed
     * @param quotaBytes     the user's total allowed quota in bytes
     * @param requestedBytes the size of the file that triggered the breach
     */
    public StorageQuotaExceededException(long usedBytes, long quotaBytes, long requestedBytes) {
        super(String.format(
                "Storage quota exceeded. Used: %.1f MB, Quota: %.1f MB, Requested: %.1f MB",
                usedBytes      / 1_048_576.0,
                quotaBytes     / 1_048_576.0,
                requestedBytes / 1_048_576.0
        ));
        this.usedBytes      = usedBytes;
        this.quotaBytes     = quotaBytes;
        this.requestedBytes = requestedBytes;
    }

    /** @return bytes already consumed by this user */
    public long getUsedBytes() {
        return usedBytes;
    }

    /** @return the user's total quota in bytes */
    public long getQuotaBytes() {
        return quotaBytes;
    }

    /** @return file size in bytes that triggered the quota breach */
    public long getRequestedBytes() {
        return requestedBytes;
    }
}
