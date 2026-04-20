package com.cloudvault.activity.repository;

import com.cloudvault.domain.ActivityLog;
import com.cloudvault.domain.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for {@link ActivityLog} records.
 *
 * <p>This repository is intentionally append-only: no update or delete
 * queries are provided. Activity logs form an immutable audit trail.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // ── Paginated single-dimension queries ───────────────────────────────────

    /** Returns a page of activity records for a specific file, newest-first. */
    Page<ActivityLog> findAllByFileIdOrderByCreatedAtDesc(Long fileId, Pageable pageable);

    /** Returns all activity records for a specific file, newest-first. */
    List<ActivityLog> findAllByFileIdOrderByCreatedAtDesc(Long fileId);

    /** Returns a page of activity records performed by a specific user, newest-first. */
    Page<ActivityLog> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Returns all activity records performed by a specific user, newest-first. */
    List<ActivityLog> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /** Returns a page of activity records for a specific event type, newest-first. */
    Page<ActivityLog> findAllByEventTypeOrderByCreatedAtDesc(EventType eventType, Pageable pageable);

    /** Returns a page of activity records in the given time window, newest-first. */
    Page<ActivityLog> findAllByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to, Pageable pageable);

    // ── Count queries ────────────────────────────────────────────────────────

    /** Counts all activity records for a given file. */
    long countByFileId(Long fileId);

    /** Counts all activity records performed by a given user. */
    long countByUserId(Long userId);

    /** Counts all activity records of a given event type. */
    long countByEventType(EventType eventType);

    // ── Dynamic multi-dimension filter query ─────────────────────────────────

    /**
     * Fetches a filtered, paginated page of {@link ActivityLog} entries.
     *
     * <p>Every parameter is optional ({@code null} disables that filter dimension).
     * Results are always ordered by {@code created_at DESC} so the most recent
     * events appear first.
     *
     * <p><strong>Note on {@code :userId IS NULL} pattern:</strong> JPQL does not
     * support {@code IS NULL} directly on {@code @Param} placeholders for non-entity
     * types in all JPA providers. The comparison is written as
     * {@code (:fileId IS NULL OR a.file.id = :fileId)} which works correctly with
     * Hibernate when the parameter is a boxed {@code Long} (nullable).
     *
     * @param fileId    filter by file PK; {@code null} = no file filter
     * @param userId    filter by user PK; {@code null} = no user filter
     * @param eventType filter by event type; {@code null} = all event types
     * @param fromDate  lower bound on {@code createdAt}; {@code null} = no lower bound
     * @param toDate    upper bound on {@code createdAt}; {@code null} = no upper bound
     * @param pageable  pagination and sort (the JPQL already forces DESC; pass an
     *                  unsorted Pageable to avoid a double ORDER BY)
     * @return paginated result slice
     */
    @Query("""
            SELECT a FROM ActivityLog a
            WHERE (:fileId IS NULL OR a.file.id = :fileId)
            AND (:userId IS NULL OR a.user.id = :userId)
            AND (:eventType IS NULL OR a.eventType = :eventType)
            AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
            AND (:toDate IS NULL OR a.createdAt <= :toDate)
            ORDER BY a.createdAt DESC
            """)
    Page<ActivityLog> findAllWithFilters(
            @Param("fileId")    Long      fileId,
            @Param("userId")    Long      userId,
            @Param("eventType") EventType eventType,
            @Param("fromDate")  Instant   fromDate,
            @Param("toDate")    Instant   toDate,
            Pageable pageable
    );
}
