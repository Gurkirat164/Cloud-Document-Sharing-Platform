package com.cloudvault.auth.service;

import com.cloudvault.auth.dto.AuthResponse;
import com.cloudvault.auth.dto.LoginRequest;
import com.cloudvault.auth.dto.RegisterRequest;
import com.cloudvault.auth.repository.RefreshTokenRepository;
import com.cloudvault.domain.RefreshToken;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.Role;
import com.cloudvault.security.JwtTokenProvider;
import com.cloudvault.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import com.cloudvault.common.exception.EmailAlreadyExistsException;

/**
 * Handles user registration, login, token refresh, and logout.
 * All write operations are transactional to ensure DB consistency.
 */
@Slf4j
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final long refreshExpiryMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.refresh-expiry-ms}") long refreshExpiryMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    /**
     * Registers a new user account and issues tokens immediately.
     *
     * @param request registration payload
     * @return AuthResponse containing access + refresh tokens and user info
     * @throws EmailAlreadyExistsException if the email is already registered
     */
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("New user registration attempt for email: {}", normalizedEmail);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String normalizedFullName = request.getFullName().trim().replaceAll("\\s+", " ");
        validatePasswordNotCompromised(request.getPassword(), normalizedEmail, normalizedFullName);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        if (!passwordEncoder.matches(request.getPassword(), encodedPassword)) {
            throw new RuntimeException("Password encoding failed");
        }

        User user = User.builder()
                .fullName(normalizedFullName)
                .email(normalizedEmail)
                .passwordHash(encodedPassword)
                .role(Role.USER)
                .storageQuota(5368709120L)
                .storageUsed(0L)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    /**
     * Authenticates a user with email + password and issues tokens.
     *
     * @param request login payload
     * @return AuthResponse containing access + refresh tokens and user info
     * @throws BadCredentialsException if the email is not found or the password is wrong
     * @throws DisabledException       if the account has been deactivated
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    /**
     * Issues a new access token for a valid, non-expired refresh token.
     * The refresh token itself is not rotated — the same one is returned.
     *
     * @param refreshTokenValue the raw refresh token string
     * @return AuthResponse with a fresh access token and the same refresh token
     * @throws IllegalArgumentException if the token is unknown or no longer active
     */
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!refreshToken.isActive()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        log.debug("Refreshed access token for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Revokes a refresh token so it can no longer be used.
     * Silently succeeds if the token is not found (already expired / deleted).
     *
     * @param refreshTokenValue the raw refresh token string
     */
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            log.info("Revoked refresh token for user: {}", token.getUser().getEmail());
        });
    }

    // ===== Private helpers =====

    private void validatePasswordNotCompromised(String password, String email, String fullName) {
        java.util.List<String> commonPasswords = java.util.Arrays.asList(
            "password", "password1", "12345678", "123456789",
            "qwerty123", "iloveyou", "admin123", "letmein1",
            "welcome1", "monkey123", "dragon123", "master123"
        );
        if (commonPasswords.contains(password)) {
            throw new IllegalArgumentException("This password is too common. Please choose a stronger password");
        }
        if (password.equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Password cannot be the same as your email");
        }
        if (password.equalsIgnoreCase(fullName)) {
            throw new IllegalArgumentException("Password cannot be the same as your name");
        }
    }

    /**
     * Generates access + refresh tokens, persists the refresh token, and builds the response.
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(user, refreshTokenValue);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Persists a new RefreshToken entity with the correct expiry.
     */
    private void saveRefreshToken(User user, String tokenValue) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpiryMs))
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}
