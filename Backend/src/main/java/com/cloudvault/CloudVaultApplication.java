package com.cloudvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CloudVault application entry point — bootstraps Spring Boot context.
 */
@SpringBootApplication
public class CloudVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudVaultApplication.class, args);
    }
}
