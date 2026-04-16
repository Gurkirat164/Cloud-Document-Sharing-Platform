package com.cloudvault.common.util;

/**
 * Utility methods for file name sanitisation and human-readable size formatting.
 */
public final class FileUtils {

    private FileUtils() {}

    /**
     * Strips unsafe characters from a filename, keeping alphanumerics, dots, underscores, and hyphens.
     */
    public static String sanitise(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Builds a namespaced S3 key: {@code users/{userUuid}/{randomUuid}-{sanitisedFilename}}.
     */
    public static String buildS3Key(String userUuid, String filename) {
        return "users/" + userUuid + "/" + java.util.UUID.randomUUID() + "-" + sanitise(filename);
    }

    /**
     * Returns a human-readable byte size, e.g. "3.1 MB".
     */
    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String unit = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), unit);
    }
}
