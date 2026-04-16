package com.cloudvault.activity.controller;

import com.cloudvault.activity.dto.ActivityLogResponse;
import com.cloudvault.activity.service.ActivityLogService;
import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints exposing paginated activity log queries for the authenticated user.
 * Effective path: /api/v1/api/activity
 */
@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
@Tag(name = "Activity", description = "User activity audit log endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    /**
     * GET /api/activity
     * Returns the authenticated user's activity log, newest first.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my activity log", description = "Returns a paginated, newest-first activity log for the authenticated user.")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityLogResponse>>> getMyActivity(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser User user) {

        Page<ActivityLogResponse> page = activityLogService.getForUser(user, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(page)));
    }
}
