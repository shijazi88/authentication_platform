package com.middleware.platform.gateway.dto;

/**
 * Allowed reasons for a fingerprint-exception verification (person physically
 * cannot provide a fingerprint). Sent by the bank; validated on the way in.
 */
public enum BiometricExceptionReason {
    HAND_INJURY,
    AMPUTATION,
    WORN_PRINTS,
    MEDICAL,
    OTHER;

    public static boolean isValid(String value) {
        if (value == null) return false;
        for (BiometricExceptionReason r : values()) {
            if (r.name().equalsIgnoreCase(value)) return true;
        }
        return false;
    }
}
