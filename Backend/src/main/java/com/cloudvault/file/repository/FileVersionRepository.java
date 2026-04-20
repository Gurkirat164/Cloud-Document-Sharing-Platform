package com.cloudvault.file.repository;

import com.cloudvault.domain.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link FileVersion} entities.
 *
 * <p>All query methods use Spring Data method-name derivation — no custom JPQL needed,
 * except for the permission existence check used by the access-control helper.
 */
@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    /**
     * Returns all versions for a file, newest first.
     *
     * @param fileId internal PK of the parent file
     * @return all versions ordered newest (highest version number) first
     */
    List<FileVersion> findAllByFileIdOrderByVersionNumberDesc(Long fileId);

    /**
     * Finds a specific version by its sequential version number.
     *
     * @param fileId        internal PK of the parent file
     * @param versionNumber 1-based sequential version number
     * @return the matching version if it exists
     */
    Optional<FileVersion> findByFileIdAndVersionNumber(Long fileId, int versionNumber);

    /**
     * Finds the version currently marked as active.
     *
     * @param fileId internal PK of the parent file
     * @return the current version, or empty if none
     */
    Optional<FileVersion> findByFileIdAndIsCurrentVersionTrue(Long fileId);

    /**
     * Counts total versions for a file.
     *
     * @param fileId internal PK of the parent file
     * @return total version count
     */
    int countByFileId(Long fileId);

    /**
     * Returns the version with the highest version number (i.e., the latest entry).
     * Used to compute the next sequential version number on new uploads.
     *
     * @param fileId internal PK of the parent file
     * @return the latest version record, or empty if no versions exist yet
     */
    Optional<FileVersion> findTopByFileIdOrderByVersionNumberDesc(Long fileId);

    /**
     * Checks whether a user has an active, non-expired permission record for a given file.
     *
     * <p>Used by {@code FileVersionService.checkReadAccess} to determine if a non-owner,
     * non-admin user may view the version history of a shared file.
     *
     * @param fileId    internal PK of the file
     * @param userId    internal PK of the user to check
     * @return {@code true} if at least one active, non-expired permission exists
     */
    @Query("""
            SELECT CASE WHEN COUNT(fp) > 0 THEN TRUE ELSE FALSE END
            FROM FilePermission fp
            WHERE fp.file.id = :fileId
              AND fp.grantee.id = :userId
              AND fp.isActive = true
              AND (fp.expiresAt IS NULL OR fp.expiresAt > CURRENT_TIMESTAMP)
            """)
    boolean existsActivePermission(@Param("fileId") Long fileId,
                                   @Param("userId")  Long userId);
}
