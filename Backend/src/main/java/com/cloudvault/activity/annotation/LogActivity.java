package com.cloudvault.activity.annotation;

import com.cloudvault.domain.enums.EventType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic activity logging via
 * {@link com.cloudvault.activity.aspect.ActivityLogAspect}.
 *
 * <p>When a method succeeds (returns normally), the aspect intercepts the
 * call, extracts context (actor, file, IP, User-Agent, sanitised arguments),
 * and asynchronously persists an {@link com.cloudvault.domain.ActivityLog}
 * entry without adding any latency to the primary request.
 *
 * <p>Usage:
 * <pre>{@code
 * @LogActivity(EventType.FILE_UPLOAD)
 * public FileResponse saveFileMetadata(FileMetadataRequest request, User user) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogActivity {

    /**
     * The semantic event type to record in the audit log.
     *
     * @return the {@link EventType} constant representing this operation
     */
    EventType value();
}
