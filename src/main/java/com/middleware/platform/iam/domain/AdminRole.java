package com.middleware.platform.iam.domain;

public enum AdminRole {
    SUPER_ADMIN,
    PLATFORM_OPS,
    FINANCE,
    SUPPORT,
    // Read-only regulator (central bank) oversight: sees clients, transactions
    // (metadata; PII stays redacted), reports and subscriptions — no writes,
    // no billing/wallets, no user management, no operational config.
    CENTRAL_BANK,
    AUDITOR
}
