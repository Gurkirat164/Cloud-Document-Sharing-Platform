package com.cloudvault.auth.dto;

import com.cloudvault.common.validation.PasswordStrength;
import com.cloudvault.common.validation.PasswordsMatch;
import com.cloudvault.common.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Payload for POST /auth/register.
 */
@Getter
@PasswordsMatch
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Full name can only contain letters, spaces, hyphens and apostrophes")
    private String fullName;

    @NotBlank(message = "Email is required")
    @ValidEmail
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @PasswordStrength
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
