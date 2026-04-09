package com.cloudvault.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Returned by register, login, and refresh endpoints.
 */
@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;

    @Builder.Default
    private final String tokenType = "Bearer";

    private final String email;
    private final String fullName;
    private final String role;
}
