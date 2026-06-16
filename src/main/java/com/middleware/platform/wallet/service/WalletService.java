package com.middleware.platform.wallet.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.wallet.domain.TenantWallet;
import com.middleware.platform.wallet.domain.WalletEntrySource;
import com.middleware.platform.wallet.domain.WalletEntryType;
import com.middleware.platform.wallet.domain.WalletLedgerEntry;
import com.middleware.platform.wallet.repo.TenantWalletRepository;
import com.middleware.platform.wallet.repo.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Prepaid wallet operations. Balance changes lock the wallet row
 * (SELECT … FOR UPDATE) so concurrent debits/credits serialize, and every
 * change is journalled to {@link WalletLedgerEntry}. Transaction debits and
 * reversals are idempotent on the transaction id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private static final String DEFAULT_CURRENCY = "YER";

    private final TenantWalletRepository wallets;
    private final WalletLedgerRepository ledger;

    @Transactional
    public TenantWallet getOrCreate(UUID tenantId, String currency) {
        return wallets.findByTenantId(tenantId).orElseGet(() ->
                wallets.save(TenantWallet.builder()
                        .tenantId(tenantId)
                        .balanceMinor(0)
                        .currency(currency == null ? DEFAULT_CURRENCY : currency)
                        .build()));
    }

    @Transactional(readOnly = true)
    public TenantWallet get(UUID tenantId) {
        return wallets.findByTenantId(tenantId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "Wallet not found"));
    }

    @Transactional(readOnly = true)
    public long balance(UUID tenantId) {
        return wallets.findByTenantId(tenantId).map(TenantWallet::getBalanceMinor).orElse(0L);
    }

    @Transactional(readOnly = true)
    public Page<WalletLedgerEntry> listLedger(UUID tenantId, Pageable pageable) {
        return ledger.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    /**
     * Fast, non-locking pre-check used at authorization to fail a call early
     * when the wallet clearly can't cover it. The authoritative check happens
     * in {@link #reserve}.
     */
    @Transactional(readOnly = true)
    public void assertCanDebit(UUID tenantId, long amountMinor) {
        if (amountMinor <= 0) return;
        long bal = balance(tenantId);
        if (bal < amountMinor) {
            throw new ApplicationException(ErrorCode.INSUFFICIENT_FUNDS,
                    "Wallet balance is insufficient for this transaction");
        }
    }

    /**
     * Atomically debit the wallet for a transaction (the "reserve" before the
     * connector call). Idempotent on the transaction id. Throws
     * INSUFFICIENT_FUNDS if the (locked) balance can't cover the amount.
     */
    @Transactional
    public TenantWallet reserve(UUID tenantId, long amountMinor, String currency, UUID transactionId) {
        if (amountMinor <= 0) return getOrCreate(tenantId, currency);
        String ref = transactionId.toString();
        if (ledger.existsByReferenceAndEntryType(ref, WalletEntryType.DEBIT)) {
            return get(tenantId); // already reserved — idempotent
        }
        getOrCreate(tenantId, currency);
        TenantWallet w = wallets.findByTenantIdForUpdate(tenantId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "Wallet not found"));
        if (w.getBalanceMinor() < amountMinor) {
            throw new ApplicationException(ErrorCode.INSUFFICIENT_FUNDS,
                    "Wallet balance is insufficient for this transaction");
        }
        long after = w.getBalanceMinor() - amountMinor;
        w.setBalanceMinor(after);
        wallets.save(w);
        record(w, WalletEntryType.DEBIT, -amountMinor, after, WalletEntrySource.SYSTEM,
                ref, "Transaction " + ref, "system");
        return w;
    }

    /**
     * Credit back a previously reserved amount when the transaction fails.
     * Idempotent on the transaction id; a no-op if nothing was reserved or it
     * was already reversed.
     */
    @Transactional
    public void reverse(UUID tenantId, long amountMinor, UUID transactionId) {
        if (amountMinor <= 0) return;
        String ref = transactionId.toString();
        if (!ledger.existsByReferenceAndEntryType(ref, WalletEntryType.DEBIT)) return;
        if (ledger.existsByReferenceAndEntryType(ref, WalletEntryType.REVERSAL)) return;
        TenantWallet w = wallets.findByTenantIdForUpdate(tenantId).orElse(null);
        if (w == null) return;
        long after = w.getBalanceMinor() + amountMinor;
        w.setBalanceMinor(after);
        wallets.save(w);
        record(w, WalletEntryType.REVERSAL, amountMinor, after, WalletEntrySource.SYSTEM,
                ref, "Reversal for transaction " + ref, "system");
    }

    /** Add funds (admin credit or payment top-up). */
    @Transactional
    public TenantWallet credit(UUID tenantId, long amountMinor, WalletEntrySource source,
                               String reference, String note, String createdBy) {
        if (amountMinor <= 0) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Top-up amount must be positive");
        }
        getOrCreate(tenantId, DEFAULT_CURRENCY);
        TenantWallet w = wallets.findByTenantIdForUpdate(tenantId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "Wallet not found"));
        long after = w.getBalanceMinor() + amountMinor;
        w.setBalanceMinor(after);
        wallets.save(w);
        record(w, WalletEntryType.TOPUP, amountMinor, after, source, reference, note, createdBy);
        return w;
    }

    private void record(TenantWallet w, WalletEntryType type, long amountMinor, long balanceAfter,
                        WalletEntrySource source, String reference, String note, String createdBy) {
        ledger.save(WalletLedgerEntry.builder()
                .walletId(w.getId())
                .tenantId(w.getTenantId())
                .entryType(type)
                .amountMinor(amountMinor)
                .balanceAfterMinor(balanceAfter)
                .currency(w.getCurrency())
                .source(source)
                .reference(reference)
                .note(note)
                .createdBy(createdBy)
                .build());
        log.info("Wallet {} {} {} → balance {}", w.getTenantId(), type, amountMinor, balanceAfter);
    }
}
