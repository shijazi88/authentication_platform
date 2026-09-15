package com.middleware.platform.gateway.imagecheck;

/** Fingerprint image container detected from the first bytes. */
public enum ImageFormat {
    /** FBI WSQ (starts with FF A0). */
    WSQ,
    /** PNG (starts with 89 50 4E 47 0D 0A 1A 0A). */
    PNG,
    /** Anything else — rejected. */
    UNKNOWN
}
