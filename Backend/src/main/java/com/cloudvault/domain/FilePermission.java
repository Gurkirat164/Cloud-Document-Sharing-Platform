package com.cloudvault.domain;

import com.cloudvault.domain.enums.Permission;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "file_permissions")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FilePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grantee_id", nullable = false)
    private User grantee;

    @Enumerated(EnumType.STRING)
    private Permission permission;

    @ManyToOne(optional = false)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    private Instant expiresAt;

    @Builder.Default
    private boolean isActive = true;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }
}
