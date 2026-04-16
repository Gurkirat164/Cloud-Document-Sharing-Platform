package com.cloudvault.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so that {@code @CreatedDate} and
 * {@code @LastModifiedDate} fields on entities are auto-populated.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
