package com.cloudvault.activity.aspect;

import com.cloudvault.activity.service.ActivityLogService;
import com.cloudvault.access.dto.PermissionResponse;
import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import com.cloudvault.file.dto.FileResponse;
import com.cloudvault.file.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AOP aspect that intercepts key CloudVault service methods and delegates to
 * {@link ActivityLogService#log} to record audit events.
 *
 * <h2>Design constraints</h2>
 * <ul>
 *   <li>Every advice method is wrapped in a {@code try-catch} — the aspect must
 *       <em>never</em> propagate exceptions into the intercepted method's call stack.</li>
 *   <li>Pointcuts use fully qualified class names to avoid any ambiguity with
 *       other classes that share short names.</li>
 *   <li>The aspect uses {@code @AfterReturning} so events are only recorded when
 *       the target method completes normally (no log entry on failure/rollback).</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
public class ActivityLogAspect {

    private final ActivityLogService activityLogService;
    private final FileRepository     fileRepository;

    public ActivityLogAspect(ActivityLogService activityLogService,
                              FileRepository fileRepository) {
        this.activityLogService = activityLogService;
        this.fileRepository     = fileRepository;
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    /**
     * Extracts the client IP address from the current HTTP request.
     * Checks {@code X-Forwarded-For} first (reverse proxy header) then falls
     * back to {@link HttpServletRequest#getRemoteAddr()}.
     *
     * @return IP address string, or {@code "unknown"} if no request is bound to the
     *         current thread
     */
    private String extractIp() {
        try {
            HttpServletRequest request = currentRequest();
            if (request == null) return "unknown";
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    /**
     * Extracts the {@code User-Agent} header from the current HTTP request.
     *
     * @return the User-Agent string, or {@code null} if unavailable
     */
    private String extractUserAgent() {
        try {
            HttpServletRequest request = currentRequest();
            return request != null ? request.getHeader("User-Agent") : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Retrieves the currently authenticated {@link User} from the Spring Security context.
     *
     * @return the {@link User} principal, or {@code null} for anonymous access
     */
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
                return null;
            }
            return (User) auth.getPrincipal();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Helper to get the current {@link HttpServletRequest} from the request context. */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * Looks up a {@link File} entity by its public UUID.
     *
     * @param uuid the file's public UUID string
     * @return the {@link File} entity, or {@code null} if not found
     */
    private File resolveFile(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        return fileRepository.findByUuid(uuid).orElse(null);
    }

    /**
     * Builds a compact JSON metadata string for a file event.
     *
     * @param fileName the original file name (may be {@code null})
     * @return a JSON string such as {@code {"fileName":"report.pdf"}}
     */
    private String fileMetadata(String fileName) {
        if (fileName == null) return null;
        return "{\"fileName\":\"" + fileName.replace("\"", "\\\"") + "\"}";
    }

    // ── FILE_UPLOAD — FileService.saveFileMetadata ────────────────────────────

    /**
     * Records a {@link EventType#FILE_UPLOAD} event after
     * {@code FileService.saveFileMetadata} completes successfully.
     *
     * <p>The {@code FileResponse} returned by the method carries the UUID used to
     * resolve the persisted {@link File} entity, and the original file name for
     * the metadata payload.
     *
     * @param jp           the intercepted join point (provides method arguments)
     * @param fileResponse the {@code FileResponse} returned by the intercepted method
     */
    @AfterReturning(
        pointcut = "execution(* com.cloudvault.file.service.FileService.saveFileMetadata(..))",
        returning = "fileResponse"
    )
    public void afterFileUpload(JoinPoint jp, FileResponse fileResponse) {
        try {
            User   actor    = getCurrentUser();
            File   file     = resolveFile(fileResponse != null ? fileResponse.uuid() : null);
            String metadata = fileMetadata(fileResponse != null ? fileResponse.originalName() : null);

            activityLogService.log(actor, file, EventType.FILE_UPLOAD, metadata);

        } catch (Exception ex) {
            log.warn("ActivityLogAspect failed to log {} event: {}", EventType.FILE_UPLOAD, ex.getMessage(), ex);
        }
    }

    // ── FILE_DELETE — FileService.deleteFile ──────────────────────────────────

    /**
     * Records a {@link EventType#FILE_DELETE} event after
     * {@code FileService.deleteFile} completes successfully.
     *
     * <p>The first {@code String} argument to {@code deleteFile} is the file UUID.
     *
     * @param jp the intercepted join point
     */
    @AfterReturning("execution(* com.cloudvault.file.service.FileService.deleteFile(..))")
    public void afterFileDelete(JoinPoint jp) {
        try {
            String fileUuid = extractFirstStringArg(jp);
            User   actor    = getCurrentUser();
            File   file     = resolveFile(fileUuid);
            String metadata = fileMetadata(file != null ? file.getOriginalName() : null);

            activityLogService.log(actor, file, EventType.FILE_DELETE, metadata);

        } catch (Exception ex) {
            log.warn("ActivityLogAspect failed to log {} event: {}", EventType.FILE_DELETE, ex.getMessage(), ex);
        }
    }

    // ── FILE_DOWNLOAD — S3Service.generatePresignedGetUrl ────────────────────

    /**
     * Records a {@link EventType#FILE_DOWNLOAD} event after a presigned GET URL
     * is generated (indicating the user is about to download a file).
     *
     * <p>The first {@code String} argument is assumed to be the S3 key; file lookup
     * falls back gracefully if no match is found.
     *
     * @param jp the intercepted join point
     */
    @AfterReturning("execution(* com.cloudvault.file.service.S3Service.generatePresignedGetUrl(..))")
    public void afterFileDownload(JoinPoint jp) {
        try {
            // The first String argument to generatePresignedGetUrl is the S3 key.
            // We attempt to find the file via the s3Key; fall back to null if not found.
            String s3Key = extractFirstStringArg(jp);
            User   actor = getCurrentUser();
            // S3Service doesn't expose a findByS3Key query; pass null file and log the key.
            String metadata = s3Key != null ? "{\"s3Key\":\"" + s3Key + "\"}" : null;

            activityLogService.log(actor, null, EventType.FILE_DOWNLOAD, metadata);

        } catch (Exception ex) {
            log.warn("ActivityLogAspect failed to log {} event: {}", EventType.FILE_DOWNLOAD, ex.getMessage(), ex);
        }
    }

    // ── PERMISSION_GRANT — PermissionService.grantPermission ─────────────────

    /**
     * Records a {@link EventType#PERMISSION_GRANT} event after
     * {@code PermissionService.grantPermission} completes successfully.
     *
     * @param jp               the intercepted join point
     * @param permissionResponse the {@code PermissionResponse} returned by the method
     */
    @AfterReturning(
        pointcut = "execution(* com.cloudvault.access.service.PermissionService.grantPermission(..))",
        returning = "permissionResponse"
    )
    public void afterPermissionGrant(JoinPoint jp, PermissionResponse permissionResponse) {
        try {
            String fileUuid = extractFirstStringArg(jp);
            User   actor    = getCurrentUser();
            File   file     = resolveFile(fileUuid);

            String metadata = permissionResponse != null
                    ? "{\"granteeEmail\":\"" + permissionResponse.getGranteeEmail() + "\"}"
                    : null;

            activityLogService.log(actor, file, EventType.PERMISSION_GRANT, metadata);

        } catch (Exception ex) {
            log.warn("ActivityLogAspect failed to log {} event: {}", EventType.PERMISSION_GRANT, ex.getMessage(), ex);
        }
    }

    // ── PERMISSION_REVOKE — PermissionService.revokePermission ───────────────

    /**
     * Records a {@link EventType#PERMISSION_REVOKE} event after
     * {@code PermissionService.revokePermission} completes successfully.
     *
     * @param jp the intercepted join point
     */
    @AfterReturning("execution(* com.cloudvault.access.service.PermissionService.revokePermission(..))")
    public void afterPermissionRevoke(JoinPoint jp) {
        try {
            String fileUuid = extractFirstStringArg(jp);
            User   actor    = getCurrentUser();
            File   file     = resolveFile(fileUuid);

            activityLogService.log(actor, file, EventType.PERMISSION_REVOKE, null);

        } catch (Exception ex) {
            log.warn("ActivityLogAspect failed to log {} event: {}", EventType.PERMISSION_REVOKE, ex.getMessage(), ex);
        }
    }

    // ── USER_LOGIN — AuthService.login ────────────────────────────────────────

    /**
     * Records a {@link EventType#USER_LOGIN} event after {@code AuthService.login}
     * completes successfully.
     *
     * <p>The authenticated user is extracted from the return value's email field
     * rather than from the security context (the context may not yet be set when
     * this advice fires for a login operation).
     *
     * @param jp           the intercepted join point
     * @param authResponse the {@code AuthResponse} returned by the login method
     */
    @AfterReturning(
        pointcut = "execution(* com.cloudvault.auth.service.AuthService.login(..))",
        returning = "authResponse"
    )
    public void afterLogin(JoinPoint jp, Object authResponse) {
        try {
            // The authenticated User may not be in the security context yet at this point.
            // Log with null user; the email is recorded in metadata instead.
            String metadata = null;
            if (authResponse != null) {
                try {
                    // Reflectively read email from AuthResponse record/bean
                    java.lang.reflect.Method getEmail = authResponse.getClass().getMethod("getEmail");
                    String email = (String) getEmail.invoke(authResponse);
                    metadata = email != null ? "{\"email\":\"" + email + "\"}" : null;
                } catch (Exception ignored) { /* reflection unavailable */ }
            }
            activityLogService.log(null, null, EventType.USER_LOGIN, metadata);

        } catch (Exception ex) {
            log.warn("ActivityLogAspect failed to log {} event: {}", EventType.USER_LOGIN, ex.getMessage(), ex);
        }
    }

    // ── USER_LOGOUT — AuthService.logout ─────────────────────────────────────

    /**
     * Records a {@link EventType#USER_LOGOUT} event after {@code AuthService.logout}
     * completes successfully.
     *
     * @param jp the intercepted join point
     */
    @AfterReturning("execution(* com.cloudvault.auth.service.AuthService.logout(..))")
    public void afterLogout(JoinPoint jp) {
        try {
            User actor = getCurrentUser();
            activityLogService.log(actor, null, EventType.USER_LOGOUT, null);

        } catch (Exception ex) {
            log.warn("ActivityLogAspect failed to log {} event: {}", EventType.USER_LOGOUT, ex.getMessage(), ex);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns the first {@link String} argument from the intercepted method's
     * argument list, or {@code null} if none is present.
     *
     * @param jp the join point providing the argument array
     * @return first String argument, or {@code null}
     */
    private String extractFirstStringArg(JoinPoint jp) {
        if (jp.getArgs() == null) return null;
        for (Object arg : jp.getArgs()) {
            if (arg instanceof String s) return s;
        }
        return null;
    }
}
