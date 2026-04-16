package com.cloudvault.activity.dto;

import com.cloudvault.domain.enums.EventType;

import java.time.Instant;

/**
 * Response DTO for a single activity log entry.
 */
public record ActivityLogResponse(
        Long id,
        EventType eventType,
        String actorEmail,
        String fileUuid,
        String fileName,
        String ipAddress,
        Instant createdAt
) {}
