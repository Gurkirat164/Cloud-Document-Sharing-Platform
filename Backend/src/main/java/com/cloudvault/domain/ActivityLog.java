package com.cloudvault.domain;

import com.cloudvault.domain.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Immutable audit trail entry. Application code must NEVER UPDATE or DELETE rows.
 * user_id and file_id use SET NULL on FK delete so logs outlive their subjects.
 */
@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")           // nullable — anonymous share link access
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")           // nullable — non-file events (LOGIN etc.)
    private File file;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_link_id")
    private ShareLink shareLink;

    /** Free-form JSON string for event-specific context (stored as TEXT). */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }
}
