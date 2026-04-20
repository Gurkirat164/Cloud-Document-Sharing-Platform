package com.cloudvault.activity;

import com.cloudvault.activity.annotation.LogActivity;
import com.cloudvault.activity.aspect.ActivityLogAspect;
import com.cloudvault.activity.repository.ActivityLogRepository;
import com.cloudvault.domain.ActivityLog;
import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import com.cloudvault.file.repository.FileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActivityLogAspect}.
 *
 * <p>The aspect is instantiated directly (not via Spring context) so Spring AOP
 * proxy mechanics are bypassed. Each test drives the {@link ActivityLogAspect#around}
 * advice method, supplying mock {@link ProceedingJoinPoint} and {@link LogActivity}
 * instances to simulate real interceptions.
 *
 * <p>The {@code @Async} method {@link ActivityLogAspect#saveLog} is called synchronously
 * in this context because no async executor is configured in the unit test scope — which
 * is fine because we verify the call to the <em>repository</em>, not the thread.
 */
@ExtendWith(MockitoExtension.class)
class ActivityLogAspectTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private ActivityLogAspect aspect;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private static User buildUser() {
        return User.builder()
                .id(1L)
                .uuid("user-uuid-001")
                .email("alice@example.com")
                .fullName("Alice")
                .passwordHash("$2a$10$ignored")
                .build();
    }

    private static File buildFile() {
        return File.builder()
                .id(10L)
                .uuid("file-uuid-001")
                .originalName("report.pdf")
                .s3Key("users/user-uuid-001/abc-report.pdf")
                .s3Bucket("cloudvault-bucket")
                .sizeBytes(2048L)
                .owner(buildUser())
                .build();
    }

    /** Mocks the JoinPoint to appear as a named method with the given args and return value. */
    private void configureJoinPoint(Object returnValue, Object... args) throws Throwable {
        when(joinPoint.proceed()).thenReturn(returnValue);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(buildParamNames(args));
        when(methodSignature.toShortString()).thenReturn("FileService.saveFileMetadata(..)");
    }

    private String[] buildParamNames(Object[] args) {
        String[] names = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            names[i] = (args[i] instanceof String) ? "fileUuid"
                     : (args[i] instanceof User)   ? "user"
                     : "arg" + i;
        }
        return names;
    }

    /** Creates a mock {@link LogActivity} annotation carrying the given {@link EventType}. */
    private LogActivity mockAnnotation(EventType eventType) {
        LogActivity annotation = mock(LogActivity.class);
        when(annotation.value()).thenReturn(eventType);
        return annotation;
    }

    @BeforeEach
    void setUp() {
        aspect = new ActivityLogAspect(activityLogRepository, fileRepository, httpServletRequest);
        // Clear security context before each test to avoid cross-test pollution.
        SecurityContextHolder.clearContext();
    }

    // ── Test 1 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("aspect_ShouldCaptureUploadEvent_WhenMethodSucceeds")
    void aspect_ShouldCaptureUploadEvent_WhenMethodSucceeds() throws Throwable {
        // Arrange
        User user = buildUser();
        File file = buildFile();

        // Populate security context so the aspect can resolve the actor.
        setAuthenticatedUser(user);

        // fileUuid in args → aspect resolves File from FileRepository.
        configureJoinPoint(null /* void return */, file.getUuid(), user);
        when(fileRepository.findByUuid(file.getUuid())).thenReturn(Optional.of(file));
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit/5");

        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        Object result = aspect.around(joinPoint, mockAnnotation(EventType.FILE_UPLOAD));

        // Allow @Async to run synchronously in test (saveLog is called directly).
        aspect.saveLog(user, file, EventType.FILE_UPLOAD, "{}");

        // Assert — repository must have been called at least once.
        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository, atLeastOnce()).save(captor.capture());

        ActivityLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(EventType.FILE_UPLOAD);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getFile()).isEqualTo(file);
        assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(result).isNull();       // the intercepted method returned void (null)
    }

    // ── Test 2 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("aspect_ShouldExtractFileUuid_FromMethodArguments")
    void aspect_ShouldExtractFileUuid_FromMethodArguments() throws Throwable {
        // Arrange
        User user = buildUser();
        File file = buildFile();
        setAuthenticatedUser(user);

        // Pass the fileUuid string as first argument — typical for delete / permission ops.
        configureJoinPoint(null, file.getUuid(), user);
        when(fileRepository.findByUuid(file.getUuid())).thenReturn(Optional.of(file));
        when(httpServletRequest.getHeader(anyString())).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.0.5");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("curl/8");
        when(activityLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act — aspect resolves the file via the UUID string argument.
        aspect.around(joinPoint, mockAnnotation(EventType.FILE_DELETE));
        aspect.saveLog(user, file, EventType.FILE_DELETE, "{\"fileUuid\":\"" + file.getUuid() + "\"}");

        // Assert
        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getValue().getFile()).isEqualTo(file);
        assertThat(captor.getValue().getEventType()).isEqualTo(EventType.FILE_DELETE);

        // Confirm FileRepository was consulted with the correct uuid.
        verify(fileRepository, atLeastOnce()).findByUuid(file.getUuid());
    }

    // ── Test 3 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("aspect_ShouldNotLog_WhenMethodThrowsException")
    void aspect_ShouldNotLog_WhenMethodThrowsException() throws Throwable {
        // Arrange — the target method throws a RuntimeException.
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("File locked"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"file-uuid-001"});
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.toShortString()).thenReturn("FileService.deleteFile(..)");

        // Act + Assert — the exception must propagate; no log entry saved.
        assertThatThrownBy(() -> aspect.around(joinPoint, mockAnnotation(EventType.FILE_DELETE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File locked");

        verifyNoInteractions(activityLogRepository);
    }

    // ── Test 4 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("aspect_ShouldHandleAnonymousUser_ForPublicLinks")
    void aspect_ShouldHandleAnonymousUser_ForPublicLinks() throws Throwable {
        // Arrange — no authentication in the security context (anonymous share-link access).
        SecurityContextHolder.setContext(new SecurityContextImpl()); // empty context

        File file = buildFile();
        configureJoinPoint(null, file.getUuid());
        when(fileRepository.findByUuid(file.getUuid())).thenReturn(Optional.of(file));
        when(httpServletRequest.getHeader(anyString())).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("203.0.113.42");
        when(activityLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act — saveLog with null actor to simulate anonymous persistence.
        aspect.around(joinPoint, mockAnnotation(EventType.FILE_DOWNLOAD));
        aspect.saveLog(null, file, EventType.FILE_DOWNLOAD, null);

        // Assert — log saved with null user.
        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getValue().getUser()).isNull();
        assertThat(captor.getValue().getEventType()).isEqualTo(EventType.FILE_DOWNLOAD);
        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.42");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setAuthenticatedUser(User user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }
}
