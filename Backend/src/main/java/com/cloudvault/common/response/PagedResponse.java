package com.cloudvault.common.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic wrapper for paginated list responses.
 *
 * <p>Exposes content, zero-based page metadata, and navigation flags so
 * clients can drive their own pagination controls without inspecting
 * Spring's internal {@link Page} object.
 *
 * @param <T> element type of the content list
 */
@Getter
@Builder
public class PagedResponse<T> {

    /** The records on the current page. */
    private final List<T> content;

    /** Zero-based page index. */
    private final int page;

    /** Number of records requested per page. */
    private final int size;

    /** Total number of records across all pages. */
    private final long totalElements;

    /** Total number of pages given the current page size. */
    private final int totalPages;

    /** {@code true} if this is the last page (no next page exists). */
    private final boolean last;

    /** {@code true} if this is the first page (no previous page exists). */
    private final boolean first;

    // ── Factory methods ──────────────────────────────────────────────────────

    /**
     * Convenience factory that derives metadata from a Spring {@link Page} and
     * uses the same, already-mapped content list.
     *
     * @param page          Spring Page (used for metadata only — content is ignored)
     * @param mappedContent separately mapped content list (e.g. DTOs mapped from entities)
     * @param <T>           element type
     * @return a new {@link PagedResponse} populated from the Spring Page
     */
    public static <T> PagedResponse<T> from(Page<?> page, List<T> mappedContent) {
        return PagedResponse.<T>builder()
                .content(mappedContent)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    /**
     * Convenience factory for cases where the page content type already matches
     * the desired DTO type (no separate mapping step needed).
     *
     * @param page Spring Page whose content is used directly
     * @param <T>  element type
     * @return a new {@link PagedResponse} populated from the Spring Page
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return from(page, page.getContent());
    }
}
