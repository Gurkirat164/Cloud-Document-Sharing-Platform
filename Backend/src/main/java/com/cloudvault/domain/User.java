package com.cloudvault.domain;

import com.cloudvault.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapped to the `users` table. Implements UserDetails so the entity
 * itself can be used as the Spring Security principal — no separate wrapper class needed.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false, columnDefinition = "CHAR(36)", unique = true)
    private String uuid;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "storage_used", nullable = false)
    @Builder.Default
    private Long storageUsed = 0L;

    @Column(name = "storage_quota", nullable = false)
    @Builder.Default
    private Long storageQuota = 5368709120L;   // 5 GB default

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ===== Lifecycle callbacks =====

    @PrePersist
    private void prePersist() {
        this.uuid = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // ===== UserDetails implementation =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    /** Spring Security uses this as the credential to verify. */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** Spring Security uses this as the principal identifier during authentication. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Maps to is_active column — disabled accounts cannot authenticate. */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }
}
