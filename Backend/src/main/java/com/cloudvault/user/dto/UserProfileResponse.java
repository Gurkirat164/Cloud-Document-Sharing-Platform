package com.cloudvault.user.dto;

import com.cloudvault.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Read-only projection of a User entity returned by profile and admin endpoints.
 */
@Getter
@Builder
public class UserProfileResponse {

    private final String uuid;
    private final String email;
    private final String fullName;
    private final String role;
    private final long storageUsed;
    private final long storageQuota;
    private final double storageUsedPercent;
    private final boolean isActive;
    private final Instant createdAt;

    /**
     * Maps a User entity to a UserProfileResponse.
     * Computes storageUsedPercent rounded to 1 decimal place.
     *
     * @param user source entity
     * @return populated response DTO
     */
    public static UserProfileResponse from(User user) {
        double pct = user.getStorageQuota() > 0
                ? Math.round(user.getStorageUsed() * 1000.0 / user.getStorageQuota()) / 10.0
                : 0.0;

        return UserProfileResponse.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .storageUsed(user.getStorageUsed())
                .storageQuota(user.getStorageQuota())
                .storageUsedPercent(pct)
                .isActive(Boolean.TRUE.equals(user.getIsActive()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
