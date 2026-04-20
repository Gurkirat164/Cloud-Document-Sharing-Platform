package com.cloudvault.file.dto;

import com.cloudvault.domain.enums.EventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

/**
 * Query parameters for searching and filtering a user's file list.
 *
 * <p>All filter fields are optional — omitted fields apply no filter for that dimension.
 * Pagination and sort fields carry sensible defaults so callers never have to supply them.
 *
 * <p>Used as {@code @ModelAttribute} in the controller so Spring MVC binds individual
 * query-string parameters directly onto this object.
 */
@Getter
@Setter
public class FileSearchRequest {

    // ── Allowed sort columns & directions (validated in service) ──────────────

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "uploadedAt", "originalName", "sizeBytes", "mimeType"
    );

    private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("ASC", "DESC");

    // ── Filter fields ─────────────────────────────────────────────────────────

    /**
     * Partial, case-insensitive match against {@code original_name}.
     * Omitting this field returns files with any name.
     */
    private String query;

    /**
     * Exact MIME-type match (e.g. {@code image/png}, {@code application/pdf}).
     * Ignored when blank.
     */
    private String mimeType;

    /**
     * MIME-type category prefix match (e.g. {@code image}, {@code video}, {@code text}).
     * Translates to {@code LIKE 'category/%'} in the query.
     * Ignored when blank.
     */
    private String mimeTypeCategory;

    /** Lower bound on upload date — files uploaded on or after this instant. */
    private Instant fromDate;

    /** Upper bound on upload date — files uploaded on or before this instant. */
    private Instant toDate;

    /** Minimum file size in bytes (inclusive). */
    private Long minSize;

    /** Maximum file size in bytes (inclusive). */
    private Long maxSize;

    /**
     * When {@code true} and the caller is {@code ADMIN}, soft-deleted files are
     * included in results. Ignored for non-ADMIN callers.
     */
    private boolean includeDeleted = false;

    // ── Sort & pagination ─────────────────────────────────────────────────────

    /**
     * Column to sort by.
     * Allowed: {@code uploadedAt}, {@code originalName}, {@code sizeBytes}, {@code mimeType}.
     * The service silently resets an invalid value to {@code uploadedAt}.
     */
    private String sortBy = "uploadedAt";

    /**
     * Sort direction — {@code ASC} or {@code DESC}.
     * The service silently resets an invalid value to {@code DESC}.
     */
    private String sortDirection = "DESC";

    /** Zero-based page index. */
    @Min(0)
    private int page = 0;

    /**
     * Records per page.
     * Minimum 1, maximum 100 to prevent runaway queries.
     */
    @Min(1)
    @Max(100)
    private int size = 20;

    // ── Validation helpers (called by the service layer) ──────────────────────

    /**
     * Returns {@code true} if {@link #sortBy} is one of the four allowed column names.
     *
     * @return {@code true} when the sort field is valid
     */
    public boolean isValidSortBy() {
        return sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy);
    }

    /**
     * Returns {@code true} if {@link #sortDirection} is {@code ASC} or {@code DESC}
     * (case-sensitive).
     *
     * @return {@code true} when the sort direction is valid
     */
    public boolean isValidSortDirection() {
        return sortDirection != null && ALLOWED_SORT_DIRECTIONS.contains(sortDirection.toUpperCase());
    }
}
