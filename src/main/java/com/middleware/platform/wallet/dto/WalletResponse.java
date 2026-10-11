package com.middleware.platform.wallet.dto;

import com.middleware.platform.wallet.domain.TenantWallet;

import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID tenantId,
        long balanceMinor,
        String currency,
        Long lowBalanceThresholdMinor,
        /** The service never stops for lack of balance; the balance may go negative (amount owed). */
        boolean unlimited,
        Instant updatedAt
) {
    public static WalletResponse from(TenantWallet w, boolean unlimited) {
        return new WalletResponse(
                w.getTenantId(),
                w.getBalanceMinor(),
                w.getCurrency(),
                w.getLowBalanceThresholdMinor(),
                unlimited,
                w.getUpdatedAt()
        );
    }
}
