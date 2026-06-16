package com.middleware.platform.wallet.dto;

import com.middleware.platform.wallet.domain.TenantWallet;

import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID tenantId,
        long balanceMinor,
        String currency,
        Long lowBalanceThresholdMinor,
        Instant updatedAt
) {
    public static WalletResponse from(TenantWallet w) {
        return new WalletResponse(
                w.getTenantId(),
                w.getBalanceMinor(),
                w.getCurrency(),
                w.getLowBalanceThresholdMinor(),
                w.getUpdatedAt()
        );
    }
}
