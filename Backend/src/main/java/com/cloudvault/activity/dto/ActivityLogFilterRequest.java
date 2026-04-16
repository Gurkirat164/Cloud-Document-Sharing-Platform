package com.cloudvault.activity.dto;

import com.cloudvault.domain.enums.EventType;

import java.time.Instant;

/**
 * Query parameters for filtering activity log results.
 * All fields are optional — omitted fields apply no filter.
 */
public record ActivityLogFilterRequest(
        EventType eventType,
        String fileUuid,
        Instant from,
        Instant to
) {}
