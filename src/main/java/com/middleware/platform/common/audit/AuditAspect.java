package com.middleware.platform.common.audit;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that automatically captures every mutating admin action and
 * writes it to the audit_log table via {@link AuditService}.
 *
 * <p>Intercepts: all public methods in any {@code @RestController} class under
 * the {@code com.middleware.platform.*.api} packages whose request mapping
 * starts with /admin/ AND whose HTTP method mutates state (POST, PUT, DELETE).
 * GET requests are excluded — they're read-only and would generate noise.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(
            pointcut = "execution(public * com.middleware.platform..api..*Controller.*(..))",
            returning = "result")
    public void auditAdminAction(JoinPoint joinPoint, Object result) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            HttpServletRequest request = attrs.getRequest();

            // Only audit /admin/** mutating calls
            String uri = request.getRequestURI();
            if (!uri.startsWith("/admin/")) return;
            String method = request.getMethod();
            if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) return;

            String action = method + " " + uri;
            String targetType = deriveTargetType(uri);
            String targetId = deriveTargetId(result);

            // Extract actor from Spring Security context
            String actorId = null;
            String actorEmail = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String email) {
                actorEmail = email;
            }

            String ipAddress = request.getRemoteAddr();

            auditService.record(actorId, actorEmail, action, targetType, targetId, null, ipAddress);
        } catch (Exception ex) {
            log.debug("Audit aspect failed (non-fatal): {}", ex.getMessage());
        }
    }

    private String deriveTargetType(String uri) {
        // Extract the resource name from the URI: /admin/tenants/xxx → TENANT
        String[] parts = uri.replace("/admin/", "").split("/");
        if (parts.length == 0) return "UNKNOWN";
        return switch (parts[0]) {
            case "tenants" -> "TENANT";
            case "plans" -> "PLAN";
            case "subscriptions" -> "SUBSCRIPTION";
            case "auth" -> "AUTH";
            default -> parts[0].toUpperCase();
        };
    }

    private String deriveTargetId(Object result) {
        if (result == null) return null;
        try {
            var method = result.getClass().getMethod("getId");
            Object id = method.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
