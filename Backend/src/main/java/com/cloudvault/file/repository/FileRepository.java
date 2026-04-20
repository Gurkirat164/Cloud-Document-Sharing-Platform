package com.cloudvault.file.repository;

import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link File} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to unlock
 * {@code findAll(Specification, Pageable)} — required by the dynamic search feature
 * ({@code FileSpecification.buildFromRequest}).
 */
@Repository
public interface FileRepository extends JpaRepository<File, Long>, JpaSpecificationExecutor<File> {

    // ── Existing methods (do not remove) ─────────────────────────────────────

    /** Find all non-deleted files belonging to a specific user, paginated. */
    Page<File> findByOwnerAndIsDeletedFalse(User owner, Pageable pageable);

    /** Find a file by its public UUID, regardless of deletion state. */
    Optional<File> findByUuid(String uuid);

    /** Find a file by UUID that is not soft-deleted. */
    Optional<File> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * Finds a file by its public UUID and the internal PK of its owner.
     * Useful for ownership verification in a single DB round-trip.
     */
    Optional<File> findByUuidAndOwnerId(String uuid, Long ownerId);

    /**
     * Counts all non-deleted files owned by a given user.
     * Used for the per-user storage stats endpoint.
     */
    long countByOwnerIdAndIsDeletedFalse(Long ownerId);

    /**
     * Returns the 5 most recently uploaded non-deleted files for a given user,
     * sorted newest-first. Used by the dashboard "Recent Files" widget.
     */
    List<File> findTop5ByOwnerIdAndIsDeletedFalseOrderByUploadedAtDesc(Long ownerId);

    // ── New method ────────────────────────────────────────────────────────────

    /**
     * Sums the {@code size_bytes} of all non-deleted files owned by a user.
     *
     * <p>Returns {@code 0} (via {@code COALESCE}) when the user has no files,
     * avoiding a {@code null} result that would require null-checking at call sites.
     *
     * <p>Used by the storage recalculation repair tool in
     * {@code UserService.recalculateStorageUsed()}.
     *
     * @param ownerId internal PK of the user
     * @return total bytes of all active (non-deleted) files for this user
     */
    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM File f WHERE f.owner.id = :ownerId AND f.isDeleted = false")
    long sumSizeByOwnerIdAndIsDeletedFalse(@Param("ownerId") Long ownerId);
}
