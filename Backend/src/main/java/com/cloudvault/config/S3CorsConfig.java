package com.cloudvault.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

/**
 * Sets the CORS policy on the CloudVault S3 bucket at startup so that
 * browsers (dev and prod) can perform direct PUT uploads via presigned URLs.
 * This is idempotent — safe to call on every boot.
 */
@Slf4j
@Configuration
public class S3CorsConfig {

    @Bean
    public ApplicationRunner applyS3CorsPolicy(S3Client s3Client,
                                               @Value("${aws.s3.bucket-name}") String bucketName) {
        return args -> {
            try {
                CORSRule rule = CORSRule.builder()
                        .allowedOrigins(
                                "http://localhost:4173",
                                "http://localhost:4174",
                                "http://localhost:5173",
                                "http://localhost:3000",
                                "http://127.0.0.1:5173",
                                "http://localhost:*",
                                "https://*.cloudvault.io"
                        )
                        .allowedMethods(
                                "GET", "PUT", "POST", "DELETE", "HEAD"
                        )
                        .allowedHeaders("*")
                        .exposeHeaders("ETag", "x-amz-version-id")
                        .maxAgeSeconds(3600)
                        .build();

                PutBucketCorsRequest request = PutBucketCorsRequest.builder()
                        .bucket(bucketName)
                        .corsConfiguration(CORSConfiguration.builder()
                                .corsRules(rule)
                                .build())
                        .build();

                s3Client.putBucketCors(request);
                log.info("S3 CORS policy applied successfully to bucket: {}", bucketName);

            } catch (Exception ex) {
                log.warn("Failed to apply S3 CORS policy to bucket '{}': {}. " +
                         "Direct browser uploads may be blocked by S3 CORS. " +
                         "Set the policy manually in the AWS Console.",
                        bucketName, ex.getMessage());
            }
        };
    }
}
