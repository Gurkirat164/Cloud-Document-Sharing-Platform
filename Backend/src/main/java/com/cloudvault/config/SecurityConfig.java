package com.cloudvault.config;

import com.cloudvault.security.JwtAuthenticationFilter;
import com.cloudvault.security.UserPrincipalService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the CloudVault API.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>CSRF disabled — stateless JWT API, no browser sessions</li>
 *   <li>Session management STATELESS — no HttpSession created or used</li>
 *   <li>/auth/**, Swagger, and OpenAPI docs are permit-all</li>
 *   <li>Admin endpoints are restricted at the filter-chain level AND method level</li>
 * </ul>
 *
 * <hr>
 * <h3>Standard @PreAuthorize Expressions — use these consistently across all modules</h3>
 * <pre>
 *   isAuthenticated()                    — any logged-in user
 *   hasRole('ADMIN')                     — ROLE_ADMIN only  (Spring auto-prepends ROLE_)
 *   hasRole('USER')                      — ROLE_USER only
 *   hasAnyRole('ADMIN', 'USER')          — either role
 *   @roleGuard.isOwner(#user, #id)       — ownership check via RoleGuard Spring bean
 *   @roleGuard.isAdminOrOwner(#user, #id)— either admin or owner
 * </pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserPrincipalService userPrincipalService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // ── Public endpoints ──────────────────────────────────────────
                    .requestMatchers(
                            "/auth/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/api-docs/**",
                            "/api/share-links/*/resolve",
                            "/api/public/share-links/*"  // public share link resolution
                    ).permitAll()

                    // ── Admin-only routes (filter-chain level) ───────────────────
                    .requestMatchers(HttpMethod.GET,    "/api/admin/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/admin/**").hasRole("ADMIN")

                    // ── Authenticated user routes ────────────────────────────────
                    .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()

                    // ── Catch-all: any other request must be authenticated ────────
                    .anyRequest().authenticated())
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userPrincipalService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
