package com.cloudvault.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Payload for PUT /api/users/me — only fullName is changeable.
 * Email and password changes require dedicated, security-reviewed flows.
 */
@Getter
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;
}
