package com.cloudvault.access.repository;

import com.cloudvault.domain.FilePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FilePermissionRepository extends JpaRepository<FilePermission, Long> {
    Optional<FilePermission> findByFileIdAndGranteeId(Long fileId, Long granteeId);
    List<FilePermission> findAllByFileId(Long fileId);
    List<FilePermission> findAllByGranteeId(Long granteeId);
    List<FilePermission> findAllByFileIdAndIsActiveTrue(Long fileId);
    boolean existsByFileIdAndGranteeIdAndIsActiveTrue(Long fileId, Long granteeId);
    Optional<FilePermission> findByFileIdAndGranteeIdAndIsActiveTrue(Long fileId, Long granteeId);
    
    Optional<FilePermission> findByFileUuidAndGranteeUuidAndIsActiveTrue(String fileUuid, String granteeUuid);
    
    @Modifying 
    @Query("UPDATE FilePermission fp SET fp.isActive = false WHERE fp.file.id = :fileId")
    void deactivateAllByFileId(@Param("fileId") Long fileId);
}
