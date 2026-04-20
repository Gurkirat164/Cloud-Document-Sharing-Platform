package com.cloudvault.access.service;

import com.cloudvault.access.dto.GrantPermissionRequest;
import com.cloudvault.access.dto.PermissionResponse;
import com.cloudvault.access.repository.FilePermissionRepository;
import com.cloudvault.activity.annotation.LogActivity;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.FilePermission;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import com.cloudvault.domain.enums.Permission;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PermissionService {

    private final FilePermissionRepository filePermissionRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @LogActivity(EventType.PERMISSION_GRANT)
    public PermissionResponse grantPermission(String fileUuid, GrantPermissionRequest request, User currentUser) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the file owner can share this file");
        }

        if (Boolean.TRUE.equals(file.getIsDeleted())) {
            throw new IllegalArgumentException("Cannot share a deleted file");
        }

        User grantee = userRepository.findByEmail(request.getGranteeEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getGranteeEmail()));

        if (grantee.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot share a file with yourself");
        }

        Instant expiresAt = null;
        if (request.getExpiresInDays() != null) {
            expiresAt = Instant.now().plus(request.getExpiresInDays(), ChronoUnit.DAYS);
        }

        FilePermission permission = filePermissionRepository
                .findByFileIdAndGranteeIdAndIsActiveTrue(file.getId(), grantee.getId())
                .orElse(null);

        if (permission != null) {
            permission.setPermission(request.getPermission());
            permission.setExpiresAt(expiresAt);
        } else {
            permission = FilePermission.builder()
                    .file(file)
                    .grantee(grantee)
                    .permission(request.getPermission())
                    .grantedBy(currentUser)
                    .expiresAt(expiresAt)
                    .isActive(true)
                    .build();
        }

        FilePermission savedPermission = filePermissionRepository.save(permission);
        log.info("File {} shared with {} by {} with {} permission", 
                file.getUuid(), grantee.getEmail(), currentUser.getEmail(), request.getPermission());

        return PermissionResponse.from(savedPermission);
    }

    @LogActivity(EventType.PERMISSION_REVOKE)
    public void revokePermission(String fileUuid, Long permissionId, User currentUser) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the file owner can share this file");
        }

        FilePermission permission = filePermissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));

        if (!permission.getFile().getId().equals(file.getId())) {
            throw new IllegalArgumentException("Permission does not belong to the specified file");
        }

        permission.setActive(false);
        filePermissionRepository.save(permission);
        
        log.info("Permission {} revoked for file {} by {}", permissionId, fileUuid, currentUser.getEmail());
    }

    public List<PermissionResponse> getFilePermissions(String fileUuid, User currentUser) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the file owner can share this file");
        }

        return filePermissionRepository.findAllByFileId(file.getId())
                .stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList());
    }

    public List<PermissionResponse> getMySharedFiles(User currentUser) {
        return filePermissionRepository.findAllByGranteeId(currentUser.getId())
                .stream()
                .filter(FilePermission::isValid)
                .filter(permission -> permission.getFile() != null && !Boolean.TRUE.equals(permission.getFile().getIsDeleted()))
                .map(PermissionResponse::from)
                .collect(Collectors.toList());
    }

    public boolean checkUserHasPermission(Long fileId, Long userId, Permission requiredPermission) {
        return filePermissionRepository.findByFileIdAndGranteeIdAndIsActiveTrue(fileId, userId)
                .filter(FilePermission::isValid)
                .map(fp -> {
                    if (requiredPermission == Permission.EDIT) {
                        return fp.getPermission() == Permission.EDIT;
                    }
                    if (requiredPermission == Permission.VIEW) {
                        return fp.getPermission() == Permission.VIEW || fp.getPermission() == Permission.EDIT;
                    }
                    return false;
                })
                .orElse(false);
    }
}
