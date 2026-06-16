package com.middleware.platform.wallet.domain;

public enum WalletEntrySource {
    /** Created by an admin action in the portal. */
    ADMIN,
    /** Created by a payment-gateway top-up. */
    PAYMENT,
    /** Created automatically by the gateway (transaction debit/reversal). */
    SYSTEM
}
