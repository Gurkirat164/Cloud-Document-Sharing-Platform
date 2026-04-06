package com.cloudvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * CloudVault application entry point — bootstraps Spring Boot context.
 */
@SpringBootApplication
@EnableJpaAuditing
public class CloudVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudVaultApplication.class, args);
    }
}
