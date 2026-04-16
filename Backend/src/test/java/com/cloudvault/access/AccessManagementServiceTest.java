package com.cloudvault.access;

import com.cloudvault.access.repository.FilePermissionRepository;
import com.cloudvault.access.repository.ShareLinkRepository;
import com.cloudvault.access.service.AccessManagementService;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.FilePermission;
import com.cloudvault.domain.ShareLink;
import com.cloudvault.domain.User;
import com.cloudvault.file.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessManagementServiceTest {

    @Mock
    private FilePermissionRepository filePermissionRepository;

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private AccessManagementService accessManagementService;

    @Test
    void revokeUserPermission_success() {
        User owner = User.builder().id(1L).build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).build();
        FilePermission permission = FilePermission.builder().id(100L).isActive(true).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        when(filePermissionRepository.findByFileUuidAndGranteeUuidAndIsActiveTrue("file-uuid", "grantee-uuid"))
                .thenReturn(Optional.of(permission));

        accessManagementService.revokeUserPermission("file-uuid", "grantee-uuid", owner);

        assertFalse(permission.isActive());
        verify(filePermissionRepository).save(permission);
    }

    @Test
    void revokeUserPermission_notOwner_throwsAccessDenied() {
        User owner = User.builder().id(1L).build();
        User other = User.builder().id(2L).build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));

        assertThrows(AccessDeniedException.class, () ->
                accessManagementService.revokeUserPermission("file-uuid", "grantee-uuid", other));
    }

    @Test
    void invalidateShareLink_success() {
        User owner = User.builder().id(1L).build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).build();
        ShareLink link = ShareLink.builder().id(100L).file(file).isActive(true).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        when(shareLinkRepository.findByTokenAndIsActiveTrue("token")).thenReturn(Optional.of(link));

        accessManagementService.invalidateShareLink("file-uuid", "token", owner);

        assertFalse(link.isActive());
        verify(shareLinkRepository).save(link);
    }

    @Test
    void invalidateShareLink_wrongFile_throwsException() {
        User owner = User.builder().id(1L).build();
        File file1 = File.builder().id(10L).uuid("file-uuid").owner(owner).build();
        File file2 = File.builder().id(20L).uuid("other-file").owner(owner).build();
        ShareLink link = ShareLink.builder().id(100L).file(file2).isActive(true).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file1));
        when(shareLinkRepository.findByTokenAndIsActiveTrue("token")).thenReturn(Optional.of(link));

        assertThrows(IllegalArgumentException.class, () ->
                accessManagementService.invalidateShareLink("file-uuid", "token", owner));
    }

    @Test
    void revokeAllAccess_success() {
        User owner = User.builder().id(1L).build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));

        accessManagementService.revokeAllAccess("file-uuid", owner);

        verify(filePermissionRepository).deactivateAllByFileId(10L);
        verify(shareLinkRepository).deactivateAllByFileId(10L);
    }
}
