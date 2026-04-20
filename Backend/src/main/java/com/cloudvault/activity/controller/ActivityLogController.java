package com.cloudvault.activity.controller;

import com.cloudvault.activity.dto.ActivityLogFilterRequest;
import com.cloudvault.activity.dto.ActivityLogResponse;
import com.cloudvault.activity.service.ActivityLogService;
import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the activity audit trail to authenticated callers.
 *
 * <ul>
 *   <li>{@code GET /api/logs} — paginated, filterable log query (role-scoped).</li>
 *   <li>{@code GET /api/logs/file/{fileUuid}} — full audit trail for one file.</li>
 *   <li>{@code GET /api/logs/stats} — platform-wide statistics (ADMIN only).</li>
 *   <li>{@code GET /api/files/{fileUuid}/activity} — compatibility alias.</li>
 *   <li>{@code GET /api/activity/me} — personal history for the authenticated user.</li>
 * </ul>
 *
 * <p>All business logic and access-control checks are delegated to
 * {@link ActivityLogService}; the controller is a thin routing layer only.
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    // ── GET /api/logs ────────────────────────────────────────────────────────

    /**
     * Returns a paginated, filtered list of activity logs.
     *
     * <p>Regular users see only their own logs regardless of filter input.
     * ADMIN users may filter by any user or file.
     *
     * <p>Example: {@code GET /api/logs?page=0&size=20&fileId=123&eventType=FILE_DOWNLOAD}
     *
     * @param filter      optional filter parameters bound from query string
     * @param currentUser authenticated principal (injected by {@code @CurrentUser})
     * @return 200 OK with {@link PagedResponse} of {@link ActivityLogResponse}
     */
    @Operation(
        summary = "Get paginated activity logs",
        description = "Users see only their own logs. Admins can filter by any user or file."
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<ActivityLogResponse>>> getLogs(
            @Valid @ModelAttribute ActivityLogFilterRequest filter,
            @CurrentUser User currentUser) {

        PagedResponse<ActivityLogResponse> result =
                activityLogService.getLogsWithFilters(filter, currentUser);

        log.debug("Activity logs queried by user={} — page={} size={} total={}",
                currentUser.getEmail(), result.getPage(), result.getSize(),
                result.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── GET /api/logs/file/{fileUuid} ────────────────────────────────────────

    /**
     * Returns the full, un-paginated audit trail for a specific file.
     *
     * <p>Only the file owner or an ADMIN may call this endpoint.
     *
     * @param fileUuid    public UUID of the target file
     * @param currentUser authenticated principal
     * @return 200 OK with ordered list of {@link ActivityLogResponse}, newest first
     */
    @Operation(summary = "Get full audit trail for a specific file")
    @GetMapping("/file/{fileUuid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getFileAuditTrail(
            @PathVariable String fileUuid,
            @CurrentUser User currentUser) {

        List<ActivityLogResponse> trail =
                activityLogService.getFileAuditTrail(fileUuid, currentUser);

        log.debug("File audit trail queried: file={} by user={} — {} entries",
                fileUuid, currentUser.getEmail(), trail.size());

        return ResponseEntity.ok(ApiResponse.success(trail));
    }

    // ── GET /api/logs/stats ──────────────────────────────────────────────────

    /**
     * Returns platform-wide activity statistics.
     *
     * <p>Restricted to {@code ADMIN} users. Returns counts for uploads, downloads,
     * shares, deletes, and logins as a flat key-value map.
     *
     * @return 200 OK with {@code Map<String, Long>} wrapped in {@link ApiResponse}
     */
    @Operation(
        summary = "Get platform-wide activity statistics",
        description = "ADMIN only. Returns aggregate counts by event category."
    )
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getAdminStats() {
        Map<String, Long> stats = activityLogService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ── Compatibility aliases ────────────────────────────────────────────────

    /**
     * Alias: {@code GET /api/files/{fileUuid}/activity}.
     * Kept for compatibility with the existing React dashboard which calls this URL.
     */
    @Operation(summary = "Get full audit trail for a specific file (alias)")
    @GetMapping("/api/files/{fileUuid}/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getFileActivity(
            @PathVariable String fileUuid,
            @CurrentUser User currentUser) {
        return getFileAuditTrail(fileUuid, currentUser);
    }

    /**
     * Alias: {@code GET /api/activity/me}.
     * Returns the personal activity history for the authenticated user.
     */
    @Operation(summary = "Get personal activity history for the authenticated user")
    @GetMapping("/api/activity/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getMyActivity(
            @CurrentUser User currentUser) {

        List<ActivityLogResponse> history = activityLogService.getUserActivity(currentUser);
        log.debug("Personal activity queried by user={} — {} entries",
                currentUser.getEmail(), history.size());
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
