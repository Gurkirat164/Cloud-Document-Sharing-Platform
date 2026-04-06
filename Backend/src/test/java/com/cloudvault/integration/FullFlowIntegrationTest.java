package com.cloudvault.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-flow integration test using Testcontainers (MySQL) + MockMvc.
 * Covers: register → login → upload file → share link → download → revoke → delete.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class FullFlowIntegrationTest {
}
