package com.cloudvault.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity mapped to the `refresh_tokens` table.
 * Stores JWT refresh tokens linked to a user session.
 * Tokens are soft-invalidated via revokedAt rather than deleted immediately.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** NULL means the token is still valid and has not been revoked. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * A token is active if it has not been revoked AND has not expired.
     *
     * @return true if the token can still be used to obtain a new access token
     */
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
