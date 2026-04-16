package com.middleware.platform.common.tenant;

import java.util.UUID;

/**
 * Holds the resolved tenant for the current request thread.
 * Populated by the client-credentials authentication filter.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(TenantInfo info) { CURRENT.set(info); }

    public static TenantInfo get() { return CURRENT.get(); }

    public static UUID currentTenantId() {
        TenantInfo t = CURRENT.get();
        return t == null ? null : t.tenantId();
    }

    public static void clear() { CURRENT.remove(); }

    public record TenantInfo(UUID tenantId, String tenantName, UUID credentialId) {}
}
