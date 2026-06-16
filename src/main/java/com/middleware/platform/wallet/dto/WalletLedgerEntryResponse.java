package com.middleware.platform.wallet.dto;

import com.middleware.platform.wallet.domain.WalletEntrySource;
import com.middleware.platform.wallet.domain.WalletEntryType;
import com.middleware.platform.wallet.domain.WalletLedgerEntry;

import java.time.Instant;
import java.util.UUID;

public record WalletLedgerEntryResponse(
        UUID id,
        WalletEntryType entryType,
        long amountMinor,
        long balanceAfterMinor,
        String currency,
        WalletEntrySource source,
        String reference,
        String note,
        String createdBy,
        Instant createdAt
) {
    public static WalletLedgerEntryResponse from(WalletLedgerEntry e) {
        return new WalletLedgerEntryResponse(
                e.getId(),
                e.getEntryType(),
                e.getAmountMinor(),
                e.getBalanceAfterMinor(),
                e.getCurrency(),
                e.getSource(),
                e.getReference(),
                e.getNote(),
                e.getCreatedBy(),
                e.getCreatedAt()
        );
    }
}
