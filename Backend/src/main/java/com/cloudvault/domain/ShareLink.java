package com.cloudvault.domain;

import com.cloudvault.domain.enums.SharePermission;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "share_links")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    private SharePermission permission;

    private String passwordHash;

    private Integer maxUses;

    @Builder.Default
    private int useCount = 0;

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

    public boolean isMaxedOut() {
        return maxUses != null && useCount >= maxUses;
    }

    public boolean isValid() {
        return isActive && !isExpired() && !isMaxedOut();
    }
}
