package com.cloudvault.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wrapper for paginated list responses, exposing content, page metadata, and navigation info.
 *
 * @param <T> element type
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
