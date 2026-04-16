package com.cloudvault.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility methods for extracting client IP address and User-Agent from an HTTP request.
 */
public final class RequestUtils {

    private RequestUtils() {}

    /**
     * Returns the real client IP, honoring common reverse-proxy headers (X-Forwarded-For, X-Real-IP).
     * Falls back to {@code request.getRemoteAddr()} if no proxy headers are present.
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip)) ip = request.getHeader("X-Real-IP");
        if (isBlank(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (isBlank(ip)) ip = request.getRemoteAddr();
        // X-Forwarded-For may be a comma-separated list; take the first (client) entry
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /** Returns the User-Agent header, or null if absent. */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank() || "unknown".equalsIgnoreCase(s);
    }
}
