package com.cloudvault.access.controller;

import com.cloudvault.access.service.AccessManagementService;
import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files/{fileUuid}/access")
@RequiredArgsConstructor
public class AccessManagementController {

    private final AccessManagementService accessManagementService;

    @DeleteMapping("/user/{granteeUuid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke specific user access to a file")
    public ResponseEntity<ApiResponse<Void>> revokeUserPermission(
            @PathVariable String fileUuid,
            @PathVariable String granteeUuid,
            @CurrentUser User currentUser) {

        accessManagementService.revokeUserPermission(fileUuid, granteeUuid, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/token/{token}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Invalidate a specific public share link")
    public ResponseEntity<ApiResponse<Void>> invalidateShareLink(
            @PathVariable String fileUuid,
            @PathVariable String token,
            @CurrentUser User currentUser) {

        accessManagementService.invalidateShareLink(fileUuid, token, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke ALL sharing (users and links) for a file")
    public ResponseEntity<Void> revokeAllAccess(
            @PathVariable String fileUuid,
            @CurrentUser User currentUser) {

        accessManagementService.revokeAllAccess(fileUuid, currentUser);
        return ResponseEntity.noContent().build();
    }
}
