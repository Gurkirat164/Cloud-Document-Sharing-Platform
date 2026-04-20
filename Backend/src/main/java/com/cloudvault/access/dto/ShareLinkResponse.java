package com.cloudvault.access.dto;

import com.cloudvault.domain.ShareLink;
import com.cloudvault.domain.enums.SharePermission;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ShareLinkResponse {
    private String token;
    private String fileUuid;
    private String fileName;
    private SharePermission permission;
    private Instant expiresAt;
    private Integer maxUses;
    private int useCount;
    private boolean isActive;

    public static ShareLinkResponse from(ShareLink sl) {
        if (sl == null) return null;

        SharePermission permission = sl.getPermission() != null ? sl.getPermission().normalized() : null;
        
        return ShareLinkResponse.builder()
                .token(sl.getToken())
                .fileUuid(sl.getFile() != null ? sl.getFile().getUuid() : null)
                .fileName(sl.getFile() != null ? sl.getFile().getOriginalName() : null)
            .permission(permission)
                .expiresAt(sl.getExpiresAt())
                .maxUses(sl.getMaxUses())
                .useCount(sl.getUseCount())
                .isActive(sl.isActive())
                .build();
    }
}
