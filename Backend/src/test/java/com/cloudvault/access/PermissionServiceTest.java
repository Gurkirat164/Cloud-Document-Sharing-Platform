package com.cloudvault.access;

import com.cloudvault.access.dto.GrantPermissionRequest;
import com.cloudvault.access.repository.FilePermissionRepository;
import com.cloudvault.access.service.PermissionService;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.FilePermission;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.Permission;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PermissionServiceTest {

    @Mock
    private FilePermissionRepository filePermissionRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void grantPermission_success_newPermission() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        User grantee = User.builder().id(2L).email("grantee@test.com").build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).isDeleted(false).build();
        
        GrantPermissionRequest request = new GrantPermissionRequest();
        request.setGranteeEmail("grantee@test.com");
        request.setPermission(Permission.VIEW);

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        when(userRepository.findByEmail("grantee@test.com")).thenReturn(Optional.of(grantee));
        when(filePermissionRepository.findByFileIdAndGranteeIdAndIsActiveTrue(10L, 2L)).thenReturn(Optional.empty());
        
        FilePermission savedPerm = FilePermission.builder()
                .id(100L).file(file).grantee(grantee).permission(Permission.VIEW)
                .grantedBy(owner).isActive(true).build();
        when(filePermissionRepository.save(any(FilePermission.class))).thenReturn(savedPerm);

        var response = permissionService.grantPermission("file-uuid", request, owner);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        verify(filePermissionRepository).save(any(FilePermission.class));
    }

    @Test
    void grantPermission_success_updatesExistingPermission() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        User grantee = User.builder().id(2L).email("grantee@test.com").build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).isDeleted(false).build();

        GrantPermissionRequest request = new GrantPermissionRequest();
        request.setGranteeEmail("grantee@test.com");
        request.setPermission(Permission.EDIT);
        request.setExpiresInDays(5);

        FilePermission existingPerm = FilePermission.builder()
                .id(100L).file(file).grantee(grantee).permission(Permission.VIEW)
                .grantedBy(owner).isActive(true).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        when(userRepository.findByEmail("grantee@test.com")).thenReturn(Optional.of(grantee));
        when(filePermissionRepository.findByFileIdAndGranteeIdAndIsActiveTrue(10L, 2L)).thenReturn(Optional.of(existingPerm));
        when(filePermissionRepository.save(any(FilePermission.class))).thenReturn(existingPerm);

        var response = permissionService.grantPermission("file-uuid", request, owner);

        assertNotNull(response);
        assertEquals(Permission.EDIT.name(), response.getPermission());
        verify(filePermissionRepository).save(existingPerm);
    }

    @Test
    void grantPermission_fileNotFound_throwsResourceNotFoundException() {
        when(fileRepository.findByUuid("bad-uuid")).thenReturn(Optional.empty());
        
        User owner = User.builder().id(1L).build();
        GrantPermissionRequest request = new GrantPermissionRequest();
        
        assertThrows(ResourceNotFoundException.class, () -> 
                permissionService.grantPermission("bad-uuid", request, owner));
    }

    @Test
    void grantPermission_notFileOwner_throwsAccessDeniedException() {
        User owner = User.builder().id(1L).build();
        User otherUser = User.builder().id(2L).build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).build();
        
        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        
        GrantPermissionRequest request = new GrantPermissionRequest();
        assertThrows(AccessDeniedException.class, () -> 
                permissionService.grantPermission("file-uuid", request, otherUser));
    }

    @Test
    void grantPermission_shareWithSelf_throwsIllegalArgumentException() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).isDeleted(false).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        
        GrantPermissionRequest request = new GrantPermissionRequest();
        request.setGranteeEmail("owner@test.com");
        
        assertThrows(IllegalArgumentException.class, () -> 
                permissionService.grantPermission("file-uuid", request, owner));
    }

    @Test
    void grantPermission_deletedFile_throwsIllegalArgumentException() {
        User owner = User.builder().id(1L).build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).isDeleted(true).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        
        GrantPermissionRequest request = new GrantPermissionRequest();
        assertThrows(IllegalArgumentException.class, () -> 
                permissionService.grantPermission("file-uuid", request, owner));
    }

    @Test
    void revokePermission_success() {
        User owner = User.builder().id(1L).email("owner@test.com").build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).build();
        FilePermission permission = FilePermission.builder().id(100L).file(file).isActive(true).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        when(filePermissionRepository.findById(100L)).thenReturn(Optional.of(permission));

        permissionService.revokePermission("file-uuid", 100L, owner);

        assertFalse(permission.isActive());
        verify(filePermissionRepository).save(permission);
    }

    @Test
    void revokePermission_notOwner_throwsAccessDeniedException() {
        User owner = User.builder().id(1L).build();
        User otherUser = User.builder().id(2L).build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(owner).build();

        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));

        assertThrows(AccessDeniedException.class, () -> 
                permissionService.revokePermission("file-uuid", 100L, otherUser));
    }

    @Test
    void checkUserHasPermission_viewPermission_allowsView() {
        FilePermission fp = FilePermission.builder()
                .permission(Permission.VIEW).isActive(true).build();
        
        when(filePermissionRepository.findByFileIdAndGranteeIdAndIsActiveTrue(10L, 2L))
                .thenReturn(Optional.of(fp));

        assertTrue(permissionService.checkUserHasPermission(10L, 2L, Permission.VIEW));
    }

    @Test
    void checkUserHasPermission_editPermission_allowsView() {
        FilePermission fp = FilePermission.builder()
                .permission(Permission.EDIT).isActive(true).build();
        
        when(filePermissionRepository.findByFileIdAndGranteeIdAndIsActiveTrue(10L, 2L))
                .thenReturn(Optional.of(fp));

        assertTrue(permissionService.checkUserHasPermission(10L, 2L, Permission.VIEW));
    }

    @Test
    void checkUserHasPermission_viewPermission_blocksEdit() {
        FilePermission fp = FilePermission.builder()
                .permission(Permission.VIEW).isActive(true).build();
        
        when(filePermissionRepository.findByFileIdAndGranteeIdAndIsActiveTrue(10L, 2L))
                .thenReturn(Optional.of(fp));

        assertFalse(permissionService.checkUserHasPermission(10L, 2L, Permission.EDIT));
    }
}
