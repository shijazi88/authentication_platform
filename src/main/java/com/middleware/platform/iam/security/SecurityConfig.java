package com.middleware.platform.iam.security;

import com.middleware.platform.iam.repo.ApiCredentialRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ClientCredentialsAuthFilter clientCredentialsAuthFilter(
            ApiCredentialRepository credentialRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher) {
        return new ClientCredentialsAuthFilter(credentialRepository, tenantRepository,
                passwordEncoder, eventPublisher);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }

    /**
     * Prevents Spring Boot from auto-registering the security filters as global servlet
     * filters — they should run only inside the security chain.
     */
    @Bean
    public FilterRegistrationBean<ClientCredentialsAuthFilter> disableClientFilterAutoRegistration(
            ClientCredentialsAuthFilter f) {
        FilterRegistrationBean<ClientCredentialsAuthFilter> reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> disableJwtFilterAutoRegistration(JwtAuthFilter f) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ClientCredentialsAuthFilter clientFilter,
                                                   JwtAuthFilter jwtFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS is enabled — the actual CorsConfigurationSource bean lives
                // in CorsConfig and is picked up automatically. Allowed origins
                // come from the `platform.cors.allowed-origins` property
                // (env var CORS_ALLOWED_ORIGINS on Railway).
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin/auth/login").permitAll()
                        .requestMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "PLATFORM_OPS", "FINANCE", "AUDITOR")
                        .requestMatchers("/api/**").hasRole("TENANT")
                        .anyRequest().denyAll()
                )
                .addFilterBefore(clientFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
