package com.middleware.platform.iam.security;

import com.middleware.platform.common.tenant.TenantContext;
import com.middleware.platform.iam.domain.ApiCredential;
import com.middleware.platform.iam.domain.IpPolicy;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.domain.TenantStatus;
import com.middleware.platform.iam.event.CredentialUsedEvent;
import com.middleware.platform.iam.repo.ApiCredentialRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests against /api/** using HTTP Basic with clientId:clientSecret.
 * On success, populates the Spring Security context AND TenantContext, and
 * publishes a {@link CredentialUsedEvent} so the credential's last_used_at gets
 * updated asynchronously without adding latency to the request.
 *
 * <p>Each failure branch logs at DEBUG level so support can diagnose "why does
 * this bank get a 401" without leaking information to the caller.
 */
@Slf4j
@RequiredArgsConstructor
public class ClientCredentialsAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";

    private final ApiCredentialRepository credentialRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only run on bank-facing API paths. Skip OPTIONS so future CORS preflight
        // requests aren't authenticated against client credentials.
        return !path.startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BASIC_PREFIX)) {
            log.debug("Auth: no Basic header on {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(header.substring(BASIC_PREFIX.length())),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            log.debug("Auth: malformed base64 in Basic header");
            chain.doFilter(request, response);
            return;
        }
        int idx = decoded.indexOf(':');
        if (idx < 0) {
            log.debug("Auth: missing ':' separator in decoded credentials");
            chain.doFilter(request, response);
            return;
        }
        String clientId = decoded.substring(0, idx);
        String clientSecret = decoded.substring(idx + 1);

        Optional<ApiCredential> credOpt = credentialRepository.findByClientId(clientId);
        if (credOpt.isEmpty()) {
            log.debug("Auth: clientId={} not found", clientId);
            chain.doFilter(request, response);
            return;
        }
        ApiCredential cred = credOpt.get();
        if (!cred.isActive()) {
            log.debug("Auth: credential {} (clientId={}) is inactive", cred.getId(), clientId);
            chain.doFilter(request, response);
            return;
        }
        if (cred.getExpiresAt() != null && cred.getExpiresAt().isBefore(Instant.now())) {
            log.debug("Auth: credential {} (clientId={}) expired at {}",
                    cred.getId(), clientId, cred.getExpiresAt());
            chain.doFilter(request, response);
            return;
        }
        if (!passwordEncoder.matches(clientSecret, cred.getClientSecretHash())) {
            log.debug("Auth: secret mismatch for clientId={}", clientId);
            chain.doFilter(request, response);
            return;
        }

        // request.getRemoteAddr() is the trustworthy client IP: with
        // server.forward-headers-strategy=framework, Spring's
        // ForwardedHeaderFilter has already resolved X-Forwarded-For from the
        // trusted proxy. We deliberately do NOT parse the raw X-Forwarded-For
        // header here — a caller could spoof it to defeat the allowlist.
        // SECURITY ASSUMPTION: the app must only be reachable through a proxy
        // that overwrites (not appends) X-Forwarded-For; direct exposure would
        // re-open the spoofing path regardless of how the IP is read.
        String callerIp = request.getRemoteAddr();

        // Per-credential IP allowlist — if the key has one, the caller must match.
        if (cred.getIpAllowlist() != null && !cred.getIpAllowlist().isBlank()
                && !IpAllowlist.matches(callerIp, cred.getIpAllowlist())) {
            log.info("Auth: IP {} rejected by credential allowlist for clientId={} (allowlist={})",
                    callerIp, clientId, cred.getIpAllowlist());
            rejectIp(response);
            return;
        }

        Optional<Tenant> tenantOpt = tenantRepository.findById(cred.getTenantId());
        if (tenantOpt.isEmpty()) {
            log.warn("Auth: credential {} references missing tenant {}",
                    cred.getId(), cred.getTenantId());
            chain.doFilter(request, response);
            return;
        }
        Tenant tenant = tenantOpt.get();
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            log.debug("Auth: tenant {} ({}) is not active (status={})",
                    tenant.getId(), tenant.getCode(), tenant.getStatus());
            chain.doFilter(request, response);
            return;
        }

        // Tenant-level IP policy — approved by admins; applies to every key of
        // the tenant regardless of the per-credential allowlist above.
        if (tenant.getIpPolicy() == IpPolicy.RESTRICTED
                && !IpAllowlist.matches(callerIp, tenant.getIpAllowlist())) {
            log.info("Auth: IP {} rejected by tenant {} IP policy (allowlist={})",
                    callerIp, tenant.getCode(), tenant.getIpAllowlist());
            rejectIp(response);
            return;
        }

        TenantPrincipal principal = new TenantPrincipal(tenant.getId(), tenant.getCode(),
                cred.getId(), cred.getClientId());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_TENANT")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        TenantContext.set(new TenantContext.TenantInfo(tenant.getId(), tenant.getCode(), cred.getId()));

        // Off-the-hot-path: an async listener will UPDATE last_used_at.
        eventPublisher.publishEvent(new CredentialUsedEvent(cred.getId(), Instant.now()));

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Source-IP rejection is a permission problem with valid credentials, so it
     * answers 403 / 1201 FORBIDDEN as promised in the ICD (§3.2, AC-12) — not
     * 401, which would send the bank chasing its client secret instead of its
     * egress IP. Body mirrors {@code ApiError}.
     */
    private void rejectIp(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"errorCode\":1201,\"error\":\"FORBIDDEN\","
                + "\"message\":\"Source IP is not in the approved IP allow-list for this client\"}");
    }
}
