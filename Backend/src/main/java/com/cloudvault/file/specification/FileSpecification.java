package com.cloudvault.file.specification;

import com.cloudvault.domain.File;
import com.cloudvault.file.dto.FileSearchRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Static factory for JPA {@link Specification} predicates used to search the {@code files} table.
 *
 * <p>This is a pure utility class — instantiation is prevented. All methods return a
 * {@link Specification} that can be combined with {@code and()}/{@code or()} to build
 * arbitrary dynamic queries.
 *
 * <p>Every method handles {@code null} / blank inputs gracefully by returning a neutral
 * {@code Specification.where(null)} (which produces no SQL predicate and never affects
 * the WHERE clause).
 */
public final class FileSpecification {

    /** Utility class — no instances allowed. */
    private FileSpecification() {
        throw new UnsupportedOperationException("FileSpecification is a static utility class");
    }

    // ── Individual predicate factories ────────────────────────────────────────

    /**
     * Matches files owned by the given user id.
     *
     * @param ownerId internal PK of the owning user; {@code null} returns a no-op spec
     * @return ownership predicate or neutral spec
     */
    public static Specification<File> hasOwner(Long ownerId) {
        if (ownerId == null) return Specification.where(null);
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("owner").get("id"), ownerId);
    }

    /**
     * Partial, case-insensitive match against {@code original_name}.
     * Translates to {@code LOWER(original_name) LIKE LOWER('%query%')}.
     *
     * @param query search term; {@code null}/blank returns a no-op spec
     * @return LIKE predicate or neutral spec
     */
    public static Specification<File> nameLike(String query) {
        if (query == null || query.isBlank()) return Specification.where(null);
        String pattern = "%" + query.toLowerCase() + "%";
        return (Root<File> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) ->
                cb.like(cb.lower(root.get("originalName")), pattern);
    }

    /**
     * Exact match on {@code mime_type} (e.g. {@code image/png}).
     *
     * @param mimeType exact MIME type; {@code null}/blank returns a no-op spec
     * @return equality predicate or neutral spec
     */
    public static Specification<File> hasMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return Specification.where(null);
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("mimeType"), mimeType);
    }

    /**
     * Matches files whose {@code mime_type} starts with {@code category/}.
     * E.g. {@code category = "image"} → {@code mime_type LIKE 'image/%'}.
     *
     * @param category MIME category prefix (the part before the {@code /});
     *                 {@code null}/blank returns a no-op spec
     * @return LIKE predicate or neutral spec
     */
    public static Specification<File> hasMimeTypeCategory(String category) {
        if (category == null || category.isBlank()) return Specification.where(null);
        String pattern = category.toLowerCase() + "/%";
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.like(cb.lower(root.get("mimeType")), pattern);
    }

    /**
     * Matches files uploaded on or after {@code from}.
     *
     * @param from lower bound (inclusive); {@code null} returns a no-op spec
     * @return date range predicate or neutral spec
     */
    public static Specification<File> uploadedAfter(Instant from) {
        if (from == null) return Specification.where(null);
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.greaterThanOrEqualTo(root.get("uploadedAt"), from);
    }

    /**
     * Matches files uploaded on or before {@code to}.
     *
     * @param to upper bound (inclusive); {@code null} returns a no-op spec
     * @return date range predicate or neutral spec
     */
    public static Specification<File> uploadedBefore(Instant to) {
        if (to == null) return Specification.where(null);
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.lessThanOrEqualTo(root.get("uploadedAt"), to);
    }

    /**
     * Matches files with {@code size_bytes >= minSize}.
     *
     * @param minSize minimum size in bytes (inclusive); {@code null} returns a no-op spec
     * @return size predicate or neutral spec
     */
    public static Specification<File> sizeGreaterThan(Long minSize) {
        if (minSize == null) return Specification.where(null);
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.greaterThanOrEqualTo(root.get("sizeBytes"), minSize);
    }

    /**
     * Matches files with {@code size_bytes <= maxSize}.
     *
     * @param maxSize maximum size in bytes (inclusive); {@code null} returns a no-op spec
     * @return size predicate or neutral spec
     */
    public static Specification<File> sizeLessThan(Long maxSize) {
        if (maxSize == null) return Specification.where(null);
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.lessThanOrEqualTo(root.get("sizeBytes"), maxSize);
    }

    /**
     * Matches only non-deleted files ({@code is_deleted = false}).
     *
     * @return is_deleted predicate
     */
    public static Specification<File> isNotDeleted() {
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("isDeleted"), false);
    }

    /**
     * Matches only soft-deleted files ({@code is_deleted = true}).
     *
     * @return is_deleted predicate
     */
    public static Specification<File> isDeleted() {
        return (Root<File> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("isDeleted"), true);
    }

    // ── Composer ─────────────────────────────────────────────────────────────

    /**
     * Builds a fully composed {@link Specification} from a {@link FileSearchRequest}.
     *
     * <p>Composition rules:
     * <ol>
     *   <li>If {@code isAdmin} is {@code false} the owner filter is always forced to
     *       {@code ownerId} (the caller's own id), regardless of any request parameter.
     *       This guarantees regular users can never read other users' files.</li>
     *   <li>If {@code isAdmin} is {@code true} and {@code ownerId} is provided (non-null),
     *       the owner filter is still applied — an admin may narrow results to one user.</li>
     *   <li>{@code isNotDeleted()} is appended unless the caller is ADMIN <em>and</em>
     *       {@code request.isIncludeDeleted()} is {@code true}.</li>
     *   <li>Every other filter spec is appended only when the corresponding request field
     *       is non-null / non-blank.</li>
     * </ol>
     *
     * @param request  incoming filter parameters; must not be {@code null}
     * @param ownerId  the caller's user id; may be {@code null} only when {@code isAdmin=true}
     * @param isAdmin  whether the caller holds the ADMIN role
     * @return a composed {@link Specification} ready for
     *         {@code FileRepository.findAll(spec, pageable)}
     */
    public static Specification<File> buildFromRequest(
            FileSearchRequest request,
            Long ownerId,
            boolean isAdmin) {

        // Start with ownership — non-admins are ALWAYS scoped to their own files.
        Specification<File> spec = isAdmin
                ? Specification.where(hasOwner(ownerId))   // null ownerId → no-op hasOwner
                : Specification.where(hasOwner(ownerId));  // ownerId is forced by service layer

        // Soft-delete filter — only admins who explicitly request deleted files bypass this.
        if (!(isAdmin && request.isIncludeDeleted())) {
            spec = spec.and(isNotDeleted());
        }

        // Optional filter dimensions — each spec method handles null gracefully.
        spec = spec.and(nameLike(request.getQuery()));
        spec = spec.and(hasMimeType(request.getMimeType()));
        spec = spec.and(hasMimeTypeCategory(request.getMimeTypeCategory()));
        spec = spec.and(uploadedAfter(request.getFromDate()));
        spec = spec.and(uploadedBefore(request.getToDate()));
        spec = spec.and(sizeGreaterThan(request.getMinSize()));
        spec = spec.and(sizeLessThan(request.getMaxSize()));

        return spec;
    }
}
