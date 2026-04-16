package com.cloudvault.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / Swagger UI configuration — registers the Bearer JWT security scheme
 * so all endpoints can be tested from the Swagger UI with a real token.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudVaultOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CloudVault API")
                        .version("1.0.0")
                        .description("Cloud Document Sharing Platform — REST API")
                        .contact(new Contact()
                                .name("CloudVault Team")
                                .email("support@cloudvault.io"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your access token (without 'Bearer' prefix)")));
    }
}
