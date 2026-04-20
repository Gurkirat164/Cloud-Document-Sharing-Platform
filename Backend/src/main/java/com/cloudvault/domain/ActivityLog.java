package com.cloudvault.domain;

import com.cloudvault.domain.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Immutable audit trail entry. Application code must NEVER UPDATE or DELETE rows.
 * user_id and file_id use SET NULL on FK delete so logs outlive their subjects.
 *
 * // APPEND-ONLY: Never UPDATE or DELETE rows from this table
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

    /**
     * Nullable — for anonymous share-link access where no authenticated user exists.
     * FK uses ON DELETE SET NULL so logs outlive deleted user accounts.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Nullable — for non-file events such as LOGIN / LOGOUT.
     * FK uses ON DELETE SET NULL so logs outlive deleted files.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * Nullable — populated only when the event was initiated via a public share link.
     * FK uses ON DELETE SET NULL so logs outlive revoked share links.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_link_id")
    private ShareLink shareLink;

    /** Free-form JSON string for event-specific context (stored as JSON column in MySQL). */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /** Set once on insert — this record is append-only and must never be updated. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }
}
