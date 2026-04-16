package com.middleware.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the admin portal + any other cross-origin consumer.
 *
 * <p>Allowed origins are read from the {@code platform.cors.allowed-origins}
 * property (comma-separated). Overridable via the
 * {@code CORS_ALLOWED_ORIGINS} environment variable at runtime — set it on
 * Railway to your deployed portal origin, e.g.
 * {@code https://sannad-portal.up.railway.app}.
 *
 * <p>Dev/default: {@code http://localhost:5173} (Vite dev server).
 *
 * <p>The filter chain in {@link com.middleware.platform.iam.security.SecurityConfig}
 * has CORS disabled at the Security level; the CorsConfigurationSource bean
 * registered here is picked up by Spring MVC's built-in CORS handler.
 */
@Configuration
public class CorsConfig {

    @Value("${platform.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("X-Request-Id", "X-Error-Code", "X-Error-Msg",
                "Content-Disposition"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/admin/**", cors);
        source.registerCorsConfiguration("/api/**", cors);
        source.registerCorsConfiguration("/actuator/**", cors);
        return source;
    }
}
