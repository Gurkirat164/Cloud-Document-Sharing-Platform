package com.cloudvault.activity.dto;

import com.cloudvault.domain.enums.EventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Query parameters for filtering paginated activity log results.
 *
 * <p>All filter fields are optional — when a field is {@code null} no filter
 * is applied for that dimension. The {@code page} and {@code size} fields
 * always have defaults so callers never have to supply them explicitly.
 *
 * <p>Used as a {@code @ModelAttribute} in the controller so Spring MVC binds
 * individual HTTP query parameters directly to this object.
 */
@Getter
@Setter
public class ActivityLogFilterRequest {

    /**
     * Internal PK of the file to filter by.
     * When set for a non-ADMIN caller the service verifies file ownership.
     */
    private Long fileId;

    /**
     * Internal PK of the user to filter by.
     * Ignored for non-ADMIN callers — the service always forces their own id.
     */
    private Long userId;

    /** Filter to a specific event type (e.g. FILE_UPLOAD, USER_LOGIN). */
    private EventType eventType;

    /** Lower bound for {@code created_at} — inclusive. */
    private Instant fromDate;

    /** Upper bound for {@code created_at} — inclusive. */
    private Instant toDate;

    /** Zero-based page index. Defaults to the first page. */
    @Min(0)
    private int page = 0;

    /**
     * Number of records per page.
     * Minimum 1, maximum 100 to prevent runaway queries.
     */
    @Min(1)
    @Max(100)
    private int size = 20;
}
