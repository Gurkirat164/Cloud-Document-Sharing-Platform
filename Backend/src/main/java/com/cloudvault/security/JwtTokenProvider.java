package com.cloudvault.security;

import com.cloudvault.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT creation, validation, and claim extraction using JJWT 0.12.x.
 *
 * <p>Tokens use HS256 signing with a secret derived from the configured key.
 * The subject is the user's public UUID (never the internal DB id).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;

    @Value("${jwt.refresh-expiry-ms}")
    private long refreshTokenExpiryMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiry-ms}") long accessTokenExpiryMs) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    /**
     * Generates a signed HS256 JWT access token.
     *
     * @param user the authenticated user
     * @return compact, signed JWT string
     */
    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUuid())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpiryMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates an opaque refresh token (UUID v4 string).
     * The actual token is stored in the DB and referenced by value.
     *
     * @return random UUID string suitable for use as a refresh token
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validates a JWT token — returns false instead of throwing for any failure.
     *
     * @param token the compact JWT string
     * @return true if the token is well-formed, properly signed, and not expired
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("Unexpected error during JWT validation: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extracts the subject (user's public UUID) from a valid token.
     *
     * @param token the compact JWT string
     * @return the UUID string stored in the subject claim
     */
    public String getUuidFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the email claim from a valid token.
     *
     * @param token the compact JWT string
     * @return the email address stored in the claims
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    // ===== Private helpers =====

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
