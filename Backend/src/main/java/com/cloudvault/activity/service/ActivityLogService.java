package com.cloudvault.activity.service;

import com.cloudvault.activity.dto.ActivityLogResponse;
import com.cloudvault.activity.repository.ActivityLogRepository;
import com.cloudvault.domain.ActivityLog;
import com.cloudvault.domain.File;
import com.cloudvault.domain.ShareLink;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Persists audit log entries and exposes paginated queries.
 * All write methods are {@code @Async} so they never block the calling thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    // ── Write ──────────────────────────────────────────────────────────────

    @Async
    public void log(EventType eventType, User user, File file, String ipAddress, String userAgent) {
        try {
            ActivityLog entry = ActivityLog.builder()
                    .eventType(eventType)
                    .user(user)
                    .file(file)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            activityLogRepository.save(entry);
        } catch (Exception ex) {
            // Never let an audit failure crash the caller
            log.warn("Failed to persist activity log: eventType={} error={}", eventType, ex.getMessage());
        }
    }

    @Async
    public void log(EventType eventType, User user, File file, ShareLink shareLink,
                    String ipAddress, String userAgent) {
        try {
            ActivityLog entry = ActivityLog.builder()
                    .eventType(eventType)
                    .user(user)
                    .file(file)
                    .shareLink(shareLink)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            activityLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to persist activity log: eventType={} error={}", eventType, ex.getMessage());
        }
    }

    // ── Query ──────────────────────────────────────────────────────────────

    public Page<ActivityLogResponse> getForUser(User user, Pageable pageable) {
        return activityLogRepository
                .findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::toResponse);
    }

    // ── Mapping ───────────────────────────────────────────────────────────

    private ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getEventType(),
                log.getUser() != null ? log.getUser().getEmail() : null,
                log.getFile() != null ? log.getFile().getUuid() : null,
                log.getFile() != null ? log.getFile().getOriginalName() : null,
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
