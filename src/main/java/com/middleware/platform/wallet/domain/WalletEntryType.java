package com.middleware.platform.wallet.domain;

public enum WalletEntryType {
    /** Funds added (admin credit or successful payment). */
    TOPUP,
    /** Funds removed for a billable transaction. */
    DEBIT,
    /** A previous debit refunded (e.g. failed/voided transaction). */
    REVERSAL,
    /** Manual correction by an admin. */
    ADJUSTMENT
}
