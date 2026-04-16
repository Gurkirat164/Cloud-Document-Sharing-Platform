package com.cloudvault.access.dto;

import com.cloudvault.domain.FilePermission;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PermissionResponse {
    private Long id;
    private String fileUuid;
    private String fileName;
    private String granteeEmail;
    private String granteeName;
    private String permission;
    private String grantedByEmail;
    private Instant expiresAt;
    private boolean isActive;
    private boolean isExpired;
    private Instant createdAt;

    public static PermissionResponse from(FilePermission fp) {
        if (fp == null) {
            return null;
        }

        return PermissionResponse.builder()
                .id(fp.getId())
                .fileUuid(fp.getFile() != null ? fp.getFile().getUuid() : null)
                .fileName(fp.getFile() != null ? fp.getFile().getOriginalName() : null)
                .granteeEmail(fp.getGrantee() != null ? fp.getGrantee().getEmail() : null)
                .granteeName(fp.getGrantee() != null ? fp.getGrantee().getFullName() : null)
                .permission(fp.getPermission() != null ? fp.getPermission().name() : null)
                .grantedByEmail(fp.getGrantedBy() != null ? fp.getGrantedBy().getEmail() : null)
                .expiresAt(fp.getExpiresAt())
                .isActive(fp.isActive())
                .isExpired(fp.isExpired())
                .createdAt(fp.getCreatedAt())
                .build();
    }
}
