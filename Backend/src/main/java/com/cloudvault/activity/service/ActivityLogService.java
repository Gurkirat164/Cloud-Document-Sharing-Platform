package com.cloudvault.activity.service;

import com.cloudvault.activity.dto.ActivityLogFilterRequest;
import com.cloudvault.activity.dto.ActivityLogResponse;
import com.cloudvault.activity.repository.ActivityLogRepository;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.common.util.RequestUtils;
import com.cloudvault.domain.ActivityLog;
import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import com.cloudvault.domain.enums.Role;
import com.cloudvault.file.repository.FileRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for activity audit logging.
 *
 * <p>Provides two categories of functionality:
 * <ol>
 *   <li><b>Write path:</b> {@link #log} persists a new {@link ActivityLog} entry.
 *       Failures are silently swallowed — activity logging must never break the
 *       primary business flow.</li>
 *   <li><b>Read path:</b> filterable, paginated query APIs with role-based access
 *       control. Regular users can only see their own logs; ADMIN users can query
 *       across all users and files.</li>
 * </ol>
 */
@Slf4j
@Service
@Transactional
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final FileRepository        fileRepository;
    private final HttpServletRequest    httpServletRequest;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                               HttpServletRequest httpServletRequest) {
        this.activityLogRepository = activityLogRepository;
        // FileRepository is NOT injected here — kept for read-path calls below.
        // It is passed in through the overloaded constructor used by the service.
        this.fileRepository        = null; // set by the full constructor
        this.httpServletRequest    = httpServletRequest;
    }

    /**
     * Full constructor used when both read-path (with file ownership checks)
     * and write-path capabilities are needed.
     */
    public ActivityLogService(ActivityLogRepository activityLogRepository,
                               FileRepository fileRepository,
                               HttpServletRequest httpServletRequest) {
        this.activityLogRepository = activityLogRepository;
        this.fileRepository        = fileRepository;
        this.httpServletRequest    = httpServletRequest;
    }

    // ── Write path ───────────────────────────────────────────────────────────

    /**
     * Builds and persists an {@link ActivityLog} entry.
     *
     * <p>All parameters except {@code eventType} are nullable:
     * <ul>
     *   <li>{@code user == null} — anonymous access via a share link</li>
     *   <li>{@code file == null} — non-file event such as LOGIN / LOGOUT</li>
     *   <li>{@code metadata == null} — no extra context available</li>
     * </ul>
     *
     * <p>This method is intentionally non-throwing: any persistence failure is
     * logged at WARN level and swallowed so the caller's transaction is unaffected.
     *
     * @param user     the acting user, or {@code null} for anonymous access
     * @param file     the target file, or {@code null} for non-file events
     * @param type     semantic event type; must not be {@code null}
     * @param metadata optional sanitised JSON string with extra event context
     */
    public void log(User user, File file, EventType type, String metadata) {
        try {
            String ipAddress = RequestUtils.getClientIp(httpServletRequest);
            String userAgent = RequestUtils.getUserAgent(httpServletRequest);

            ActivityLog entry = ActivityLog.builder()
                    .user(user)
                    .file(file)
                    .eventType(type)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .metadata(metadata)
                    .build();

            activityLogRepository.save(entry);

            log.info("Activity logged: [{}] by [{}] on file [{}]",
                    type,
                    user != null ? user.getEmail() : "anonymous",
                    file != null ? file.getUuid()  : "N/A");

        } catch (Exception ex) {
            log.warn("Failed to save activity log for event {}: {}", type, ex.getMessage(), ex);
        }
    }

    // ── Read path — paginated filters ────────────────────────────────────────

    /**
     * Returns a paginated, filtered page of activity logs.
     *
     * <p>Access control rules:
     * <ul>
     *   <li>Regular {@code USER}: {@code userId} is always forced to
     *       {@code currentUser.getId()} regardless of what the caller supplies.
     *       If {@code fileId} is set, file ownership is verified.</li>
     *   <li>{@code ADMIN}: all filter dimensions pass through unmodified.</li>
     * </ul>
     *
     * @param filter      filter parameters from the HTTP request
     * @param currentUser the authenticated principal
     * @return {@link PagedResponse} of {@link ActivityLogResponse} DTOs
     * @throws AccessDeniedException if a user tries to filter by a file they don't own
     */
    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> getLogsWithFilters(
            ActivityLogFilterRequest filter,
            User currentUser) {

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        Long effectiveUserId = filter.getUserId();
        Long effectiveFileId = filter.getFileId();

        if (!isAdmin) {
            // Regular users can only see their own logs.
            effectiveUserId = currentUser.getId();

            // If a fileId filter was requested, verify the file belongs to this user.
            if (effectiveFileId != null && fileRepository != null) {
                File file = fileRepository.findById(effectiveFileId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "File not found: " + effectiveFileId));
                if (!file.getOwner().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("You can only view logs for your own files");
                }
            }
        }

        log.debug("Fetching logs with filters: fileId={}, userId={}, eventType={}",
                effectiveFileId, effectiveUserId, filter.getEventType());

        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ActivityLog> page = activityLogRepository.findAllWithFilters(
                effectiveFileId,
                effectiveUserId,
                filter.getEventType(),
                filter.getFromDate(),
                filter.getToDate(),
                pageable
        );

        List<ActivityLogResponse> content = page.getContent()
                .stream()
                .map(ActivityLogResponse::from)
                .toList();

        return PagedResponse.from(page, content);
    }

    // ── Read path — file audit trail ─────────────────────────────────────────

    /**
     * Returns the full, ordered audit trail for a specific file.
     *
     * <p>Only the file owner or an ADMIN may call this. No pagination is applied —
     * the complete trail is returned in a single list, sorted newest-first.
     *
     * @param fileUuid    public UUID of the target file
     * @param currentUser the authenticated principal
     * @return ordered {@link List} of {@link ActivityLogResponse} DTOs, newest first
     * @throws ResourceNotFoundException if the file does not exist
     * @throws AccessDeniedException     if the caller is not the file owner and not ADMIN
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getFileAuditTrail(String fileUuid, User currentUser) {
        if (fileRepository == null) {
            throw new IllegalStateException("FileRepository is required for getFileAuditTrail");
        }

        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileUuid));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = file.getOwner().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Only the file owner can view the full audit trail");
        }

        return activityLogRepository
                .findAllByFileIdOrderByCreatedAtDesc(file.getId())
                .stream()
                .map(ActivityLogResponse::from)
                .toList();
    }

    /**
     * Alias kept for compatibility with the prior controller and test implementation.
     * Delegates to {@link #getFileAuditTrail(String, User)}.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getFileActivity(String fileUuid, User currentUser) {
        return getFileAuditTrail(fileUuid, currentUser);
    }

    /**
     * Returns the personal activity history for the currently authenticated user,
     * sorted newest-first.
     *
     * @param currentUser the authenticated user whose history is requested
     * @return ordered {@link List} of {@link ActivityLogResponse} DTOs, newest first
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getUserActivity(User currentUser) {
        return activityLogRepository
                .findAllByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(ActivityLogResponse::from)
                .toList();
    }

    // ── Admin stats ──────────────────────────────────────────────────────────

    /**
     * Returns platform-wide activity counts keyed by event category.
     *
     * <p>Restricted to {@code ADMIN} users via {@code @PreAuthorize}.
     *
     * @return {@code Map<String, Long>} with keys:
     *         {@code totalUploads}, {@code totalDownloads}, {@code totalShares},
     *         {@code totalDeletes}, {@code totalLogins}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Map<String, Long> getAdminDashboardStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("totalUploads",   activityLogRepository.countByEventType(EventType.FILE_UPLOAD));
        stats.put("totalDownloads", activityLogRepository.countByEventType(EventType.FILE_DOWNLOAD));
        stats.put("totalShares",    activityLogRepository.countByEventType(EventType.PERMISSION_GRANT));
        stats.put("totalDeletes",   activityLogRepository.countByEventType(EventType.FILE_DELETE));
        stats.put("totalLogins",    activityLogRepository.countByEventType(EventType.USER_LOGIN));
        return stats;
    }
}
