package com.middleware.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * <p>Two API groups are exposed in the Swagger UI dropdown:
 * <ul>
 *   <li><b>service-provider</b> — bank-facing verification API ({@code /api/v1/**}).
 *       Authenticated with HTTP Basic using {@code clientId:clientSecret} issued
 *       by the platform admin.</li>
 *   <li><b>admin</b> — platform admin portal ({@code /admin/**}). Authenticated
 *       with a Bearer JWT obtained from {@code POST /admin/auth/login}.</li>
 * </ul>
 *
 * <p>Both security schemes are registered globally so Swagger UI's "Authorize"
 * button works for either group.
 */
@Configuration
public class OpenApiConfig {

    public static final String BASIC_SCHEME = "ClientCredentialsBasic";
    public static final String BEARER_SCHEME = "AdminBearer";

    @Bean
    public OpenAPI baseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Authentication Middleware Platform")
                        .description("Verification middleware between banks (service providers) "
                                + "and government identity backends (Yemen ID, etc.).")
                        .version("0.1.0")
                        .contact(new Contact().name("Platform Team")))
                .components(new Components()
                        .addSecuritySchemes(BASIC_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic with the client_id and client_secret "
                                        + "issued for the bank tenant"))
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Bearer JWT obtained from POST /admin/auth/login")));
    }

    /**
     * Bank-facing verification API. Uses HTTP Basic with client credentials.
     */
    @Bean
    public GroupedOpenApi serviceProviderApi() {
        return GroupedOpenApi.builder()
                .group("service-provider")
                .displayName("Service Provider API (banks)")
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .addSecurityItem(new SecurityRequirement().addList(BASIC_SCHEME))
                        .info(new Info()
                                .title("Service Provider API")
                                .version("v1")
                                .description("Endpoints consumed by bank channel applications. "
                                        + "Authenticate with HTTP Basic using the client_id "
                                        + "and client_secret issued for your tenant.")))
                .build();
    }

    /**
     * Platform admin portal API. Uses JWT bearer auth.
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin Portal API")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                        .info(new Info()
                                .title("Admin Portal API")
                                .version("v1")
                                .description("Endpoints used by the platform admin portal "
                                        + "(tenants, plans, subscriptions, transactions, billing). "
                                        + "Authenticate via POST /admin/auth/login to obtain a JWT.")))
                .build();
    }
}
