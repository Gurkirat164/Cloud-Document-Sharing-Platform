package com.cloudvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Global CORS configuration.
 * Allows the Vite dev server (localhost:5173) and any production origin
 * listed in ALLOWED_ORIGINS to call the API with Authorization headers.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Permitted origins — extend for production domains as needed
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:3000",   // CRA / alternative dev
                "http://127.0.0.1:5173",
                "http://localhost:*",      // Covers preview ports like 4173, 4174
                "https://*.cloudvault.io" // production wildcard
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
