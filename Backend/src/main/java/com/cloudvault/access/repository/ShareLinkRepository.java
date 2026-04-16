package com.cloudvault.access.repository;

import com.cloudvault.domain.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByToken(String token);
    List<ShareLink> findAllByFileId(Long fileId);

    Optional<ShareLink> findByTokenAndIsActiveTrue(String token);

    @Modifying 
    @Query("UPDATE ShareLink sl SET sl.isActive = false WHERE sl.file.id = :fileId")
    void deactivateAllByFileId(@Param("fileId") Long fileId);
}
