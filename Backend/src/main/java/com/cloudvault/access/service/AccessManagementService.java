package com.cloudvault.access.service;

import com.cloudvault.access.repository.FilePermissionRepository;
import com.cloudvault.access.repository.ShareLinkRepository;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.FilePermission;
import com.cloudvault.domain.ShareLink;
import com.cloudvault.domain.User;
import com.cloudvault.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccessManagementService {

    private final FilePermissionRepository filePermissionRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;

    public void revokeUserPermission(String fileUuid, String granteeUuid, User currentUser) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the file owner can manage access");
        }

        FilePermission permission = filePermissionRepository
                .findByFileUuidAndGranteeUuidAndIsActiveTrue(fileUuid, granteeUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Active permission not found for this user"));

        permission.setActive(false);
        permission.setUpdatedAt(Instant.now());
        filePermissionRepository.save(permission);

        log.info("User access revoked: File {} for User {}", fileUuid, granteeUuid);
    }

    public void invalidateShareLink(String fileUuid, String token, User currentUser) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the file owner can manage access");
        }

        ShareLink shareLink = shareLinkRepository.findByTokenAndIsActiveTrue(token)
                .orElseThrow(() -> new ResourceNotFoundException("Active share link not found"));

        if (!shareLink.getFile().getId().equals(file.getId())) {
            throw new IllegalArgumentException("Share link does not belong to this file");
        }

        shareLink.setActive(false);
        shareLink.setUpdatedAt(Instant.now());
        shareLinkRepository.save(shareLink);

        log.info("Share link invalidated: Token {} for File {}", token, fileUuid);
    }

    public void revokeAllAccess(String fileUuid, User currentUser) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the file owner can manage access");
        }

        filePermissionRepository.deactivateAllByFileId(file.getId());
        shareLinkRepository.deactivateAllByFileId(file.getId());

        log.warn("All external access revoked for file: {}", fileUuid);
    }
}
