package com.cloudvault.activity.dto;

import com.cloudvault.domain.ActivityLog;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Public-facing representation of an {@link ActivityLog} entry returned by the REST API.
 *
 * <p>Exposes only safe fields — internal DB ids and FK entity references are
 * never leaked. Sensitive runtime data (passwords, tokens) is never stored in
 * the source {@link ActivityLog} and therefore never appears here.
 *
 * <p>All nullable fields are explicitly null-checked in {@link #from(ActivityLog)}
 * to guarantee NPE-free mapping regardless of database state.
 */
@Getter
@Builder
public class ActivityLogResponse {

    /** Surrogate PK of the log entry. */
    private final Long id;

    /** Semantic event classification as a string (e.g. "FILE_UPLOAD"). */
    private final String eventType;

    /** Email of the acting user, or {@code null} for anonymous share-link access. */
    private final String userEmail;

    /** Full name of the acting user, or {@code null} for anonymous access. */
    private final String userFullName;

    /** Original filename at the time of the event, or {@code null} for LOGIN/LOGOUT events. */
    private final String fileName;

    /** Public UUID of the related file, or {@code null} for non-file events. */
    private final String fileUuid;

    /** Client IP address recorded at event time, or {@code null} if unavailable. */
    private final String ipAddress;

    /** User-Agent header value recorded at event time, or {@code null}. */
    private final String userAgent;

    /** Raw JSON string with optional event-specific metadata, or {@code null}. */
    private final String metadata;

    /** UTC instant when the event was recorded, set once on insert. */
    private final Instant createdAt;

    /**
     * Converts an {@link ActivityLog} JPA entity into an {@link ActivityLogResponse} DTO.
     *
     * <p>All nullable associations ({@code user}, {@code file}) are null-checked to
     * guarantee that this method never throws a {@link NullPointerException}.
     *
     * @param log the entity to convert; must not be {@code null}
     * @return an immutable {@link ActivityLogResponse} safe for JSON serialisation
     */
    public static ActivityLogResponse from(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .eventType(log.getEventType() != null ? log.getEventType().name() : null)
                .userEmail(log.getUser()     != null ? log.getUser().getEmail()        : null)
                .userFullName(log.getUser()  != null ? log.getUser().getFullName()     : null)
                .fileName(log.getFile()      != null ? log.getFile().getOriginalName() : null)
                .fileUuid(log.getFile()      != null ? log.getFile().getUuid()         : null)
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .metadata(log.getMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
