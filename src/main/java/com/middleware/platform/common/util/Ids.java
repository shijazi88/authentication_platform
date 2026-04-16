package com.middleware.platform.common.util;

import com.github.f4b6a3.uuid.UuidCreator;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * ID and secret generation utilities.
 */
public final class Ids {

    private static final SecureRandom RNG = new SecureRandom();

    private Ids() {}

    /** UUID v7 — time-ordered, recommended for transaction identifiers. */
    public static UUID uuidV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    /** Generate a random url-safe API client identifier. */
    public static String newClientId() {
        return "cli_" + randomBase64Url(12);
    }

    /** Generate a random url-safe API client secret. */
    public static String newClientSecret() {
        return "sec_" + randomBase64Url(32);
    }

    private static String randomBase64Url(int byteLen) {
        byte[] bytes = new byte[byteLen];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
