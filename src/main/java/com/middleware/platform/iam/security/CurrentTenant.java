package com.middleware.platform.iam.security;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Resolves the tenant id of the authenticated tenant-portal user from the
 * security context (set by {@link JwtAuthFilter} from the token's tenantId
 * claim). Used to scope every tenant-portal query to its own tenant.
 */
public final class CurrentTenant {

    private CurrentTenant() {}

    public static UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object details = auth != null ? auth.getDetails() : null;
        if (details instanceof UUID u) return u;
        throw new ApplicationException(ErrorCode.UNAUTHENTICATED, "No tenant context");
    }

    public static String email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
