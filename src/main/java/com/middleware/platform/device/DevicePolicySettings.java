package com.middleware.platform.device;

import jakarta.validation.constraints.NotNull;

/**
 * Admin-editable policy for the {@code deviceId} a bank sends with each
 * verification (ICD §4.2.2). Stored under platform setting {@link #KEY}.
 */
public record DevicePolicySettings(
        /** When true a verification without {@code deviceId} is rejected (400 / 1002). Exception requests are exempt. */
        boolean deviceIdRequired,
        /** ALLOW_ALL records the device only; REGISTERED_ONLY rejects serials not registered for the tenant (403 / 1205). */
        @NotNull Mode mode
) {
    public static final String KEY = "DEVICE_POLICY";

    public enum Mode { ALLOW_ALL, REGISTERED_ONLY }

    public static DevicePolicySettings defaults() {
        return new DevicePolicySettings(true, Mode.ALLOW_ALL);
    }

    public DevicePolicySettings normalized() {
        return new DevicePolicySettings(deviceIdRequired, mode == null ? Mode.ALLOW_ALL : mode);
    }
}
