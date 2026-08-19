package com.middleware.platform.iam.security;

import com.middleware.platform.iam.repo.ApiCredentialRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class SecurityConfig {

    // Role groups for the /admin/** access matrix. hasAnyRole(...) prepends ROLE_.
    // PLATFORM_OPS is surfaced in the portal as "Operation"; AUDITOR is a legacy
    // read-only role (no longer assignable) kept here so existing accounts can
    // still view data.
    private static final String SUPER = "SUPER_ADMIN";
    private static final String OPS = "PLATFORM_OPS";
    private static final String FINANCE = "FINANCE";
    private static final String SUPPORT = "SUPPORT";
    private static final String AUDITOR = "AUDITOR";
    /** Operational write roles. */
    private static final String[] WRITE_ROLES = {SUPER, OPS};
    /** Anyone who may read operational data (incl. read-only Support). */
    private static final String[] READ_ROLES = {SUPER, OPS, FINANCE, SUPPORT, AUDITOR};

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
                        // Tenant portal: public login, everything else needs a tenant token.
                        .requestMatchers(HttpMethod.POST, "/portal-api/auth/login").permitAll()
                        .requestMatchers("/portal-api/**").hasRole("TENANT_USER")
                        // Per-user PIN management — any authenticated admin.
                        .requestMatchers("/admin/auth/pin", "/admin/auth/pin/**").hasAnyRole(READ_ROLES)
                        // User management — privileged (also guarded by @PreAuthorize).
                        .requestMatchers("/admin/users/**").hasRole(SUPER)
                        // Wallets — Support may read balances/ledger (GET) to answer
                        // "insufficient funds" tickets, but never top up.
                        .requestMatchers(HttpMethod.GET, "/admin/wallets/**").hasAnyRole(SUPER, FINANCE, SUPPORT)
                        // Billing + wallets (writes) — finance + super only.
                        .requestMatchers("/admin/billing/**", "/admin/wallets/**").hasAnyRole(SUPER, FINANCE)
                        // Service catalog + connector credentials — operational config.
                        .requestMatchers("/admin/catalog/**", "/admin/moi-credentials/**").hasAnyRole(WRITE_ROLES)
                        // Operational data: anyone may read, only super/ops may write.
                        .requestMatchers(HttpMethod.GET,
                                "/admin/tenants/**", "/admin/plans/**", "/admin/subscriptions/**").hasAnyRole(READ_ROLES)
                        .requestMatchers(
                                "/admin/tenants/**", "/admin/plans/**", "/admin/subscriptions/**").hasAnyRole(WRITE_ROLES)
                        // Read-only surfaces (GET-only controllers).
                        .requestMatchers(
                                "/admin/transactions/**", "/admin/reports/**",
                                "/admin/search/**", "/admin/audit/**").hasAnyRole(READ_ROLES)
                        // Anything else under /admin is treated as an operational action.
                        .requestMatchers("/admin/**").hasAnyRole(WRITE_ROLES)
                        .requestMatchers("/api/**").hasRole("TENANT")
                        .anyRequest().denyAll()
                )
                .addFilterBefore(clientFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh
                        // Unauthenticated (missing/invalid token) → 401.
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // Authenticated but lacking the role → 403 (NOT 401), so a
                        // permission gap never looks like a dead session / logs the
                        // user out. Body mirrors the ApiError shape.
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"errorCode\":1201,\"error\":\"FORBIDDEN\",\"message\":\"Access denied\"}");
                        }))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
