package com.cloudvault.access.controller;

import com.cloudvault.access.dto.CreateShareLinkRequest;
import com.cloudvault.access.dto.ShareLinkResponse;
import com.cloudvault.access.service.ShareLinkService;
import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @PostMapping("/api/files/{fileUuid}/share-links")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> createShareLink(
            @PathVariable String fileUuid,
            @RequestBody CreateShareLinkRequest request,
            @CurrentUser User currentUser) {
        
        ShareLinkResponse response = shareLinkService.createShareLink(fileUuid, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // PermitAll in SecurityConfig is required for this endpoint to work without authentication.
    @GetMapping("/api/public/share-links/{token}")
    public ResponseEntity<ApiResponse<String>> accessShareLink(
            @PathVariable String token,
            @RequestParam(required = false) String password) {
        
        String url = shareLinkService.accessShareLink(token, password);
        return ResponseEntity.ok(ApiResponse.success(url));
    }
}
