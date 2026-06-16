package com.middleware.platform.iam.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.domain.TenantUser;
import com.middleware.platform.iam.dto.*;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.iam.repo.TenantUserRepository;
import com.middleware.platform.iam.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tenant-portal user lifecycle: portal login (issues a tenant-scoped JWT) and
 * admin-side management (create/list/activate/reset). Tenant users are created
 * by platform admins — there is no self-registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantUserService {

    private final TenantUserRepository users;
    private final TenantRepository tenants;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public TenantLoginResponse login(LoginRequest req) {
        TenantUser user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_CREDENTIALS));
        if (!user.isActive() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.setLastLoginAt(Instant.now());
        Tenant tenant = tenants.findById(user.getTenantId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "Tenant not found"));
        return new TenantLoginResponse(
                jwtService.issueTenantToken(user), "Bearer", jwtService.accessTokenTtlSeconds(),
                tenant.getId(), tenant.getLegalName(), user.getEmail(), user.getDisplayName());
    }

    @Transactional(readOnly = true)
    public TenantMeResponse me(String email) {
        TenantUser user = users.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.UNAUTHENTICATED, "Unknown user"));
        Tenant tenant = tenants.findById(user.getTenantId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "Tenant not found"));
        return new TenantMeResponse(user.getEmail(), user.getDisplayName(),
                tenant.getId(), tenant.getCode(), tenant.getLegalName());
    }

    // ── Admin-side management ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TenantUserResponse> listByTenant(UUID tenantId) {
        return users.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(TenantUserResponse::from).toList();
    }

    @Transactional
    public TenantUserResponse create(UUID tenantId, CreateTenantUserRequest req) {
        if (!tenants.existsById(tenantId)) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "Tenant not found");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "A user with this email already exists");
        }
        TenantUser user = TenantUser.builder()
                .tenantId(tenantId)
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(blankToNull(req.displayName()))
                .active(true)
                .build();
        users.save(user);
        log.info("Tenant user created: {} for tenant {}", email, tenantId);
        return TenantUserResponse.from(user);
    }

    @Transactional
    public TenantUserResponse setActive(UUID tenantId, UUID id, boolean active) {
        TenantUser user = requireInTenant(tenantId, id);
        user.setActive(active);
        return TenantUserResponse.from(user);
    }

    @Transactional
    public void resetPassword(UUID tenantId, UUID id, ResetPasswordRequest req) {
        TenantUser user = requireInTenant(tenantId, id);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        log.info("Tenant user password reset: {}", user.getEmail());
    }

    private TenantUser requireInTenant(UUID tenantId, UUID id) {
        TenantUser user = users.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "User not found"));
        if (!user.getTenantId().equals(tenantId)) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "User not found");
        }
        return user;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
