package com.cloudvault.access.dto;

import com.cloudvault.domain.enums.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GrantPermissionRequest {

    @NotBlank
    @Email
    private String granteeEmail;

    @NotNull
    private Permission permission;

    private Integer expiresInDays;
}
