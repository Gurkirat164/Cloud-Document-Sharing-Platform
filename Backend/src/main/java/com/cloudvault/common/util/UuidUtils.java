package com.cloudvault.common.util;

import java.util.UUID;

// Utility methods for generating and formatting UUIDs used as public-facing identifiers.
public final class UuidUtils {

    private UuidUtils() {}

    public static String newUuid() {
        return UUID.randomUUID().toString();
    }

    public static UUID parse(String uuid) {
        return UUID.fromString(uuid);
    }
}
