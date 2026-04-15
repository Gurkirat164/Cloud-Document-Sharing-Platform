package com.cloudvault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Builds and exposes S3Client and S3Presigner beans.
 * Supports an optional endpoint override for LocalStack in dev (aws.s3.endpoint-override).
 */
@Configuration
public class AwsS3Config {

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    /** Optional — set in application-dev.yml to point at LocalStack. Null in prod. */
    @Value("${aws.s3.endpoint-override:#{null}}")
    private String endpointOverride;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            );

        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
                   .forcePathStyle(true);   // required for LocalStack
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            );

        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }
}
