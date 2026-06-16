package com.middleware.platform.iam.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Authenticates Bearer-JWT requests for the admin portal (/admin/**) and the
 * tenant portal (/portal-api/**). Tenant tokens additionally carry a tenantId
 * claim, stashed on the authentication details for request scoping.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean jwtPath = path.startsWith("/admin/") || path.startsWith("/portal-api/");
        boolean loginPath = path.startsWith("/admin/auth/login")
                || path.startsWith("/portal-api/auth/login");
        return !jwtPath || loginPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER.length()).trim();
        try {
            Claims claims = jwtService.parse(token);
            String role = claims.get("role", String.class);
            String email = claims.get("email", String.class);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            // Tenant tokens carry the tenant id — stash it for request scoping.
            String tenantId = claims.get("tenantId", String.class);
            if (tenantId != null) {
                auth.setDetails(UUID.fromString(tenantId));
            }
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
