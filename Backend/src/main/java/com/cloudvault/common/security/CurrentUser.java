package com.cloudvault.common.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Convenience annotation for injecting the currently authenticated {@code User}
 * entity directly into a controller method parameter.
 *
 * <p>Usage:
 * <pre>
 *   &#64;GetMapping("/me")
 *   public ResponseEntity&lt;?&gt; getProfile(&#64;CurrentUser User user) { ... }
 * </pre>
 *
 * <p>Equivalent to {@code @AuthenticationPrincipal User user} but more expressive
 * and decouples controllers from the Spring Security API.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
