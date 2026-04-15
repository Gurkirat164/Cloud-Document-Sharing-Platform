package com.cloudvault.file.repository;

import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    /** Find all non-deleted files belonging to a specific user, paginated. */
    Page<File> findByOwnerAndIsDeletedFalse(User owner, Pageable pageable);

    /** Find a file by its public UUID, regardless of deletion state. */
    Optional<File> findByUuid(String uuid);

    /** Find a file by UUID that is not soft-deleted. */
    Optional<File> findByUuidAndIsDeletedFalse(String uuid);
}
