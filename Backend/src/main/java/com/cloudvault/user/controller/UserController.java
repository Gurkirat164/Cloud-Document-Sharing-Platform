package com.cloudvault.user.controller;

import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import com.cloudvault.user.dto.UpdateProfileRequest;
import com.cloudvault.user.dto.UserProfileResponse;
import com.cloudvault.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for user profile management and admin user administration.
 * All endpoints require a valid JWT — see SecurityConfig for filter-chain rules.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and admin user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // =========================================================================
    // Self-service endpoints — any authenticated user
    // =========================================================================

    /**
     * GET /api/v1/api/users/me
     * Returns the profile of the currently authenticated user.
     * Access: any authenticated user (isAuthenticated()).
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get my profile",
        description = "Returns the full profile of the currently authenticated user. Access: any logged-in user."
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @CurrentUser User user) {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(user)));
    }

    /**
     * PUT /api/v1/api/users/me
     * Updates the fullName of the currently authenticated user.
     * Access: any authenticated user (isAuthenticated()).
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Update my profile",
        description = "Updates the fullName of the currently authenticated user. Email and password are not changeable here. Access: any logged-in user."
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @CurrentUser User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated successfully",
                userService.updateProfile(user, request)));
    }

    // =========================================================================
    // Admin-only endpoints — ROLE_ADMIN required
    // =========================================================================

    /**
     * GET /api/v1/api/users
     * Returns a paginated list of all users.
     * Access: ROLE_ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "List all users",
        description = "Returns a paginated list of all users in the system. Access: ROLE_ADMIN only."
    )
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserProfileResponse> page = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(page)));
    }

    /**
     * GET /api/v1/api/users/{uuid}
     * Returns a single user by their public UUID.
     * Access: ROLE_ADMIN only.
     */
    @GetMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get user by UUID",
        description = "Returns the full profile of any user identified by their public UUID. Access: ROLE_ADMIN only."
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByUuid(
            @PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByUuid(uuid)));
    }

    /**
     * PATCH /api/v1/api/users/{uuid}/deactivate
     * Disables a user account so they can no longer log in.
     * Access: ROLE_ADMIN only.
     */
    @PatchMapping("/{uuid}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Deactivate user",
        description = "Sets isActive = false on the target user, preventing future logins. Access: ROLE_ADMIN only."
    )
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable String uuid,
            @CurrentUser User requester) {
        userService.deactivateUser(uuid, requester);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", null));
    }

    /**
     * PATCH /api/v1/api/users/{uuid}/activate
     * Re-enables a previously deactivated user account.
     * Access: ROLE_ADMIN only.
     */
    @PatchMapping("/{uuid}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Activate user",
        description = "Sets isActive = true on the target user, re-enabling login. Access: ROLE_ADMIN only."
    )
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @PathVariable String uuid,
            @CurrentUser User requester) {
        userService.activateUser(uuid, requester);
        return ResponseEntity.ok(ApiResponse.success("User activated", null));
    }
}
