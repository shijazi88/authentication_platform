package com.middleware.platform.iam.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.AdminRole;
import com.middleware.platform.iam.domain.AdminUser;
import com.middleware.platform.iam.dto.AdminUserResponse;
import com.middleware.platform.iam.dto.CreateAdminUserRequest;
import com.middleware.platform.iam.dto.LoginRequest;
import com.middleware.platform.iam.dto.LoginResponse;
import com.middleware.platform.iam.dto.PinStatusResponse;
import com.middleware.platform.iam.dto.ResetPasswordRequest;
import com.middleware.platform.iam.dto.SetPinRequest;
import com.middleware.platform.iam.dto.UnlockResponse;
import com.middleware.platform.iam.dto.UpdateAdminUserRequest;
import com.middleware.platform.iam.dto.VerifyPinRequest;
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
import java.util.List;
import java.util.UUID;

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

    // ── Admin user management (SUPER_ADMIN only — enforced at the controller) ──

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return userRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) {
        return AdminUserResponse.from(require(id));
    }

    @Transactional
    public AdminUserResponse create(CreateAdminUserRequest req) {
        assignableRole(req.role());
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "A user with this email already exists");
        }
        AdminUser user = AdminUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(blankToNull(req.displayName()))
                .role(req.role())
                .active(true)
                .build();
        userRepository.save(user);
        log.info("Admin user created: {} ({})", email, req.role());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse update(UUID id, UpdateAdminUserRequest req) {
        assignableRole(req.role());
        AdminUser user = require(id);
        // Demoting the last active super-admin would lock everyone out of user management.
        if (user.getRole() == AdminRole.SUPER_ADMIN && req.role() != AdminRole.SUPER_ADMIN) {
            assertNotLastActiveSuperAdmin(user);
        }
        user.setDisplayName(blankToNull(req.displayName()));
        user.setRole(req.role());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse setActive(UUID id, boolean active, String currentUserEmail) {
        AdminUser user = require(id);
        if (!active) {
            if (user.getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new ApplicationException(ErrorCode.CONFLICT, "You cannot deactivate your own account");
            }
            if (user.getRole() == AdminRole.SUPER_ADMIN) {
                assertNotLastActiveSuperAdmin(user);
            }
        }
        user.setActive(active);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest req) {
        AdminUser user = require(id);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        log.info("Admin user password reset: {}", user.getEmail());
    }

    // ── Step-up PIN (per-user, secures the Transactions page) ─────────────────

    @Transactional(readOnly = true)
    public PinStatusResponse pinStatus(String email) {
        AdminUser user = requireByEmail(email);
        return new PinStatusResponse(user.getPinHash() != null);
    }

    @Transactional
    public void setPin(String email, SetPinRequest req) {
        AdminUser user = requireByEmail(email);
        // Changing an existing PIN requires the current one.
        if (user.getPinHash() != null) {
            if (req.currentPin() == null || !passwordEncoder.matches(req.currentPin(), user.getPinHash())) {
                throw new ApplicationException(ErrorCode.INVALID_PIN, "Current PIN is incorrect");
            }
        }
        user.setPinHash(passwordEncoder.encode(req.pin()));
        log.info("Step-up PIN set for {}", email);
    }

    @Transactional(readOnly = true)
    public UnlockResponse verifyPin(String email, VerifyPinRequest req) {
        AdminUser user = requireByEmail(email);
        if (user.getPinHash() == null) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "No PIN is set for this account");
        }
        if (!passwordEncoder.matches(req.pin(), user.getPinHash())) {
            throw new ApplicationException(ErrorCode.INVALID_PIN, "Invalid PIN");
        }
        return new UnlockResponse(jwtService.issueUnlockToken(email), jwtService.unlockTtlSeconds());
    }

    private AdminUser requireByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.UNAUTHENTICATED, "Unknown user"));
    }

    private AdminUser require(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "User not found"));
    }

    /** AUDITOR is a legacy role that is no longer assignable through the portal. */
    private void assignableRole(AdminRole role) {
        if (role != AdminRole.SUPER_ADMIN && role != AdminRole.PLATFORM_OPS
                && role != AdminRole.FINANCE && role != AdminRole.SUPPORT) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Role is not assignable");
        }
    }

    private void assertNotLastActiveSuperAdmin(AdminUser user) {
        boolean lastOne = user.isActive()
                && userRepository.countByRoleAndActiveTrue(AdminRole.SUPER_ADMIN) <= 1;
        if (lastOne) {
            throw new ApplicationException(ErrorCode.CONFLICT, "At least one active super-admin is required");
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
