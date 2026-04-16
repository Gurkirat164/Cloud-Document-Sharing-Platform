package com.cloudvault.access.controller;

import com.cloudvault.access.dto.GrantPermissionRequest;
import com.cloudvault.access.dto.PermissionResponse;
import com.cloudvault.access.service.PermissionService;
import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping("/{fileUuid}/share")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Share a file with another user", description = "Only the file owner can share. Grants VIEW or EDIT permission.")
    public ResponseEntity<ApiResponse<PermissionResponse>> shareFile(
            @PathVariable String fileUuid,
            @Valid @RequestBody GrantPermissionRequest request,
            @CurrentUser User currentUser) {
        
        PermissionResponse response = permissionService.grantPermission(fileUuid, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{fileUuid}/share/{permissionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke a previously granted file permission")
    public ResponseEntity<Void> revokePermission(
            @PathVariable String fileUuid,
            @PathVariable Long permissionId,
            @CurrentUser User currentUser) {
        
        permissionService.revokePermission(fileUuid, permissionId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fileUuid}/share")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all users who have access to a file")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getFilePermissions(
            @PathVariable String fileUuid,
            @CurrentUser User currentUser) {
        
        List<PermissionResponse> permissions = permissionService.getFilePermissions(fileUuid, currentUser);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/shared-with-me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all files shared with the current user")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getSharedWithMe(
            @CurrentUser User currentUser) {
        
        List<PermissionResponse> permissions = permissionService.getMySharedFiles(currentUser);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }
}
