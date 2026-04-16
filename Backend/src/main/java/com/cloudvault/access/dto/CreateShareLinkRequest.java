package com.cloudvault.access.dto;

import com.cloudvault.domain.enums.SharePermission;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateShareLinkRequest {
    private UUID fileUuid;
    private SharePermission permission;
    private Integer expirationHours;
    private String password;
    private Integer maxUses;
}
