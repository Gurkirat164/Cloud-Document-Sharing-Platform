package com.cloudvault.activity;

import com.cloudvault.activity.aspect.ActivityLogAspect;
import com.cloudvault.activity.dto.ActivityLogFilterRequest;
import com.cloudvault.activity.dto.ActivityLogResponse;
import com.cloudvault.activity.repository.ActivityLogRepository;
import com.cloudvault.activity.service.ActivityLogService;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.common.util.RequestUtils;
import com.cloudvault.domain.ActivityLog;
import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import com.cloudvault.domain.enums.Role;
import com.cloudvault.file.dto.FileResponse;
import com.cloudvault.file.repository.FileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActivityLogService} and the AOP wiring of
 * {@link ActivityLogAspect}.
 *
 * <p>All Spring context and database infrastructure is replaced with Mockito
 * doubles so each test runs in pure JVM without an embedded container.
 */
@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    // ── Mocks for ActivityLogService ─────────────────────────────────────────

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    // ── Mocks for ActivityLogAspect ──────────────────────────────────────────

    @Mock
    private ActivityLogService mockActivityLogService;

    @Mock
    private FileRepository fileRepository;

    // ── Systems under test ───────────────────────────────────────────────────

    private ActivityLogService activityLogService;
    private ActivityLogAspect  activityLogAspect;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private static User buildUser() {
        return User.builder()
                .id(1L)
                .uuid("user-uuid-001")
                .email("alice@example.com")
                .fullName("Alice")
                .passwordHash("$2a$10$hashedpass")
                .role(Role.USER)
                .build();
    }

    private static User buildAdmin() {
        return User.builder()
                .id(99L)
                .uuid("admin-uuid-001")
                .email("admin@example.com")
                .fullName("Admin")
                .passwordHash("$2a$10$hashedpass")
                .role(Role.ADMIN)
                .build();
    }

    private static File buildFile(User owner) {
        return File.builder()
                .id(10L)
                .uuid("file-uuid-001")
                .originalName("report.pdf")
                .s3Key("users/user-uuid-001/report.pdf")
                .s3Bucket("cloudvault-bucket")
                .sizeBytes(1024L)
                .owner(owner)
                .build();
    }

    @BeforeEach
    void setUp() {
        // 2-arg constructor — matches test contract; FileRepository is null
        activityLogService = new ActivityLogService(activityLogRepository, httpServletRequest);
        activityLogAspect  = new ActivityLogAspect(mockActivityLogService, fileRepository);
    }

    // ── Test 1: logEvent_success_savesLog ────────────────────────────────────

    @Test
    @DisplayName("logEvent_success: persists an ActivityLog with correct field values")
    void logEvent_success_savesLog() {
        User user = buildUser();
        File file = buildFile(user);

        try (MockedStatic<RequestUtils> requestUtils = mockStatic(RequestUtils.class)) {
            requestUtils.when(() -> RequestUtils.getClientIp(httpServletRequest))
                        .thenReturn("192.168.1.10");
            requestUtils.when(() -> RequestUtils.getUserAgent(httpServletRequest))
                        .thenReturn("Mozilla/5.0 (Test)");

            when(activityLogRepository.save(any(ActivityLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            activityLogService.log(user, file, EventType.FILE_UPLOAD, "{\"size\":1024}");

            // Assert
            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());

            ActivityLog saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getFile()).isEqualTo(file);
            assertThat(saved.getEventType()).isEqualTo(EventType.FILE_UPLOAD);
            assertThat(saved.getIpAddress()).isEqualTo("192.168.1.10");
            assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0 (Test)");
            assertThat(saved.getMetadata()).isEqualTo("{\"size\":1024}");
        }
    }

    // ── Test 2: logEvent_saveThrows_doesNotPropagateException ────────────────

    @Test
    @DisplayName("logEvent_saveThrows: logging failure is silently swallowed — caller never sees exception")
    void logEvent_saveThrows_doesNotPropagateException() {
        User user = buildUser();

        try (MockedStatic<RequestUtils> requestUtils = mockStatic(RequestUtils.class)) {
            requestUtils.when(() -> RequestUtils.getClientIp(any()))
                        .thenReturn("10.0.0.1");
            requestUtils.when(() -> RequestUtils.getUserAgent(any()))
                        .thenReturn("curl/7");

            when(activityLogRepository.save(any(ActivityLog.class)))
                    .thenThrow(new RuntimeException("DB connection lost"));

            // Act — must NOT throw
            activityLogService.log(user, null, EventType.FILE_UPLOAD, null);

            // Assert — save was attempted exactly once, but no exception propagated
            verify(activityLogRepository).save(any(ActivityLog.class));
        }
    }

    // ── Test 3: getLogsWithFilters_asUser_forcesUserIdFilter ─────────────────

    @Test
    @DisplayName("getLogsWithFilters_asUser: USER role forces userId to own id regardless of filter input")
    void getLogsWithFilters_asUser_forcesUserIdFilter() {
        User user = buildUser(); // role = USER, id = 1

        // Build a filter that requests userId = 999 (another user's id)
        ActivityLogFilterRequest filter = new ActivityLogFilterRequest();
        filter.setUserId(999L); // should be overridden
        filter.setPage(0);
        filter.setSize(10);

        Page<ActivityLog> emptyPage = new PageImpl<>(List.of());
        when(activityLogRepository.findAllWithFilters(
                isNull(),     // fileId
                eq(1L),       // userId MUST be forced to currentUser.getId()
                isNull(),     // eventType
                isNull(),     // fromDate
                isNull(),     // toDate
                any(Pageable.class)
        )).thenReturn(emptyPage);

        // Act
        PagedResponse<ActivityLogResponse> result =
                activityLogService.getLogsWithFilters(filter, user);

        // Assert — repository was called with userId = 1 (not 999)
        verify(activityLogRepository).findAllWithFilters(
                isNull(), eq(1L), isNull(), isNull(), isNull(), any(Pageable.class));

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
    }

    // ── Test 4: getLogsWithFilters_asAdmin_allowsAllFilters ──────────────────

    @Test
    @DisplayName("getLogsWithFilters_asAdmin: ADMIN role passes all filter dimensions through unchanged")
    void getLogsWithFilters_asAdmin_allowsAllFilters() {
        User admin = buildAdmin(); // role = ADMIN

        ActivityLogFilterRequest filter = new ActivityLogFilterRequest();
        filter.setUserId(42L);
        filter.setFileId(7L);
        filter.setEventType(EventType.FILE_DOWNLOAD);
        filter.setPage(0);
        filter.setSize(20);

        Page<ActivityLog> emptyPage = new PageImpl<>(List.of());
        when(activityLogRepository.findAllWithFilters(
                eq(7L),
                eq(42L),
                eq(EventType.FILE_DOWNLOAD),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        // Act
        PagedResponse<ActivityLogResponse> result =
                activityLogService.getLogsWithFilters(filter, admin);

        // Assert — all filter params passed through unchanged
        verify(activityLogRepository).findAllWithFilters(
                eq(7L), eq(42L), eq(EventType.FILE_DOWNLOAD),
                isNull(), isNull(), any(Pageable.class));

        assertThat(result).isNotNull();
    }

    // ── Test 5: getFileAuditTrail_notOwnerNotAdmin_throwsAccessDeniedException ──

    @Test
    @DisplayName("getFileAuditTrail: throws AccessDeniedException when caller is not owner and not ADMIN")
    void getFileAuditTrail_notOwnerNotAdmin_throwsAccessDeniedException() {
        User owner = buildUser();       // id = 1
        User stranger = User.builder()
                .id(2L)
                .uuid("stranger-uuid")
                .email("bob@example.com")
                .fullName("Bob")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        File file = buildFile(owner);   // owned by user id = 1

        // Use 3-arg constructor so fileRepository is wired
        ActivityLogService svc = new ActivityLogService(
                activityLogRepository, fileRepository, httpServletRequest);

        when(fileRepository.findByUuid("file-uuid-001")).thenReturn(Optional.of(file));

        // Act + Assert
        assertThatThrownBy(() -> svc.getFileAuditTrail("file-uuid-001", stranger))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("owner");

        verifyNoInteractions(activityLogRepository);
    }

    // ── Test 6: getAdminDashboardStats_returnsCorrectKeys ────────────────────

    @Test
    @DisplayName("getAdminDashboardStats: returns map with all five expected stat keys")
    void getAdminDashboardStats_returnsCorrectKeys() {
        // Use 3-arg constructor for full service
        ActivityLogService svc = new ActivityLogService(
                activityLogRepository, fileRepository, httpServletRequest);

        when(activityLogRepository.countByEventType(EventType.FILE_UPLOAD)).thenReturn(50L);
        when(activityLogRepository.countByEventType(EventType.FILE_DOWNLOAD)).thenReturn(200L);
        when(activityLogRepository.countByEventType(EventType.PERMISSION_GRANT)).thenReturn(15L);
        when(activityLogRepository.countByEventType(EventType.FILE_DELETE)).thenReturn(8L);
        when(activityLogRepository.countByEventType(EventType.USER_LOGIN)).thenReturn(300L);

        // Act
        Map<String, Long> stats = svc.getAdminDashboardStats();

        // Assert — all five keys present with correct values
        assertThat(stats).containsKeys(
                "totalUploads", "totalDownloads", "totalShares",
                "totalDeletes", "totalLogins");
        assertThat(stats).containsEntry("totalUploads",   50L);
        assertThat(stats).containsEntry("totalDownloads", 200L);
        assertThat(stats).containsEntry("totalShares",    15L);
        assertThat(stats).containsEntry("totalDeletes",   8L);
        assertThat(stats).containsEntry("totalLogins",    300L);
    }

    // ── Test 7: aspect_interceptsUpload_successfully ──────────────────────────

    @Test
    @DisplayName("aspect_interceptsUpload_successfully: ActivityLogAspect delegates FILE_UPLOAD to ActivityLogService")
    void aspect_interceptsUpload_successfully() {
        User user = buildUser();
        File file = buildFile(user);

        // The aspect resolves the file entity via the UUID in the FileResponse.
        when(fileRepository.findByUuid(file.getUuid())).thenReturn(Optional.of(file));

        FileResponse fileResponse = new FileResponse(
                file.getUuid(),
                file.getOriginalName(),
                "application/pdf",
                file.getSizeBytes(),
                file.getS3Key(),
                user.getEmail(),
                Instant.now()
        );

        JoinPoint jp = mock(JoinPoint.class);
        // saveFileMetadata(FileMetadataRequest request, User user)
        when(jp.getArgs()).thenReturn(new Object[]{ null, user });

        // Act — invoke the advice method directly (simulates AOP interception)
        activityLogAspect.afterFileUpload(jp, fileResponse);

        // Assert — service.log called once with FILE_UPLOAD and the resolved File
        verify(mockActivityLogService, times(1)).log(
                isNull(),                                      // actor (not in security context)
                argThat(f -> f != null && file.getUuid().equals(f.getUuid())),
                eq(EventType.FILE_UPLOAD),
                argThat(m -> m != null && m.contains("report.pdf"))
        );
    }

    // ── Test 8: logEvent_withNullUser_forAnonymousAccess ─────────────────────

    @Test
    @DisplayName("log_withNullUser_forAnonymousAccess: persists log with null user for share-link access")
    void log_withNullUser_forAnonymousAccess() {
        User owner = buildUser();
        File file  = buildFile(owner);

        try (MockedStatic<RequestUtils> requestUtils = mockStatic(RequestUtils.class)) {
            requestUtils.when(() -> RequestUtils.getClientIp(httpServletRequest))
                        .thenReturn("10.0.0.5");
            requestUtils.when(() -> RequestUtils.getUserAgent(httpServletRequest))
                        .thenReturn("curl/7.87.0");

            when(activityLogRepository.save(any(ActivityLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act — anonymous access: user is null
            activityLogService.log(null, file, EventType.FILE_DOWNLOAD, null);

            // Assert
            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());

            ActivityLog saved = captor.getValue();
            assertThat(saved.getUser()).isNull();
            assertThat(saved.getFile()).isEqualTo(file);
            assertThat(saved.getEventType()).isEqualTo(EventType.FILE_DOWNLOAD);
            assertThat(saved.getIpAddress()).isEqualTo("10.0.0.5");
            assertThat(saved.getMetadata()).isNull();
        }
    }
}
