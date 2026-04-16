package com.middleware.platform.iam.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.AdminRole;
import com.middleware.platform.iam.domain.AdminUser;
import com.middleware.platform.iam.dto.LoginRequest;
import com.middleware.platform.iam.dto.LoginResponse;
import com.middleware.platform.iam.repo.AdminUserRepository;
import com.middleware.platform.iam.security.JwtService;
import com.middleware.platform.iam.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;

    /**
     * Seeds the bootstrap admin user on application startup if (and only if)
     * {@code platform.security.bootstrap-admin.enabled=true}. The dev profile
     * enables this; staging/prod profiles disable it.
     *
     * <p>Bound to {@link ApplicationReadyEvent} (not {@code @PostConstruct}) so
     * that the full Spring context — including the {@code @Transactional} proxy
     * on this bean — is fully initialized before we touch the database.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedBootstrapAdmin() {
        var bootstrap = securityProperties.bootstrapAdmin();
        if (bootstrap == null || !bootstrap.enabled()) return;
        if (bootstrap.email() == null || bootstrap.email().isBlank()) return;
        if (userRepository.existsByEmail(bootstrap.email())) return;

        AdminUser admin = AdminUser.builder()
                .email(bootstrap.email())
                .passwordHash(passwordEncoder.encode(bootstrap.password()))
                .displayName("Bootstrap Admin")
                .role(AdminRole.SUPER_ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);
        log.warn("Bootstrap admin user created: {} — change the password immediately.", bootstrap.email());
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        AdminUser user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_CREDENTIALS));
        if (!user.isActive() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.setLastLoginAt(Instant.now());
        String token = jwtService.issueAccessToken(user);
        return new LoginResponse(token, "Bearer", jwtService.accessTokenTtlSeconds(), user.getRole());
    }
}
