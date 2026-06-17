package com.middleware.platform.iam.domain;

public enum EncryptionKeyStatus {
    /** Current key — handed to clients for encryption. */
    ACTIVE,
    /** Superseded but still able to decrypt in-flight requests. */
    RETIRING,
    /** No longer usable. */
    REVOKED
}
