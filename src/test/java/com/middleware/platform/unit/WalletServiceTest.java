package com.middleware.platform.unit;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.wallet.domain.TenantWallet;
import com.middleware.platform.wallet.domain.WalletEntryType;
import com.middleware.platform.wallet.domain.WalletLedgerEntry;
import com.middleware.platform.wallet.repo.TenantWalletRepository;
import com.middleware.platform.wallet.repo.WalletLedgerRepository;
import com.middleware.platform.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final long PRICE = 25_000; // 250 YER

    private TenantWalletRepository wallets;
    private WalletLedgerRepository ledger;
    private TenantRepository tenants;
    private WalletService service;
    private TenantWallet wallet;

    @BeforeEach
    void setUp() {
        wallets = mock(TenantWalletRepository.class);
        ledger = mock(WalletLedgerRepository.class);
        tenants = mock(TenantRepository.class);
        service = new WalletService(wallets, ledger, tenants);
        wallet = TenantWallet.builder().id(UUID.randomUUID()).tenantId(TENANT).balanceMinor(10_000).currency("YER").build();
        when(wallets.findByTenantId(TENANT)).thenReturn(Optional.of(wallet));
        when(wallets.findByTenantIdForUpdate(TENANT)).thenReturn(Optional.of(wallet));
    }

    @Test
    @DisplayName("prepaid: a charge the balance can't cover is rejected with 1403 and nothing is debited")
    void prepaidRejectsShortBalance() {
        assertThatThrownBy(() -> service.reserve(TENANT, PRICE, "YER", UUID.randomUUID()))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(wallet.getBalanceMinor()).isEqualTo(10_000);
        verify(ledger, never()).save(any());
    }

    @Test
    @DisplayName("unlimited: the charge is still debited and the balance goes negative")
    void unlimitedGoesNegative() {
        UUID tx = UUID.randomUUID();
        service.reserve(TENANT, PRICE, "YER", tx, true);

        assertThat(wallet.getBalanceMinor()).isEqualTo(-15_000);
        ArgumentCaptor<WalletLedgerEntry> entry = ArgumentCaptor.forClass(WalletLedgerEntry.class);
        verify(ledger).save(entry.capture());
        assertThat(entry.getValue().getEntryType()).isEqualTo(WalletEntryType.DEBIT);
        assertThat(entry.getValue().getAmountMinor()).isEqualTo(-PRICE);
        assertThat(entry.getValue().getBalanceAfterMinor()).isEqualTo(-15_000);
        assertThat(entry.getValue().getReference()).isEqualTo(tx.toString());
    }

    @Test
    @DisplayName("unlimited: a failed call is refunded as usual")
    void unlimitedRefund() {
        UUID tx = UUID.randomUUID();
        service.reserve(TENANT, PRICE, "YER", tx, true);
        when(ledger.existsByReferenceAndEntryType(tx.toString(), WalletEntryType.DEBIT)).thenReturn(true);
        service.reverse(TENANT, PRICE, tx);
        assertThat(wallet.getBalanceMinor()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("isUnlimited reads the tenant flag (false for unknown tenants)")
    void isUnlimited() {
        Tenant t = Tenant.builder().id(TENANT).walletUnlimited(true).build();
        when(tenants.findById(TENANT)).thenReturn(Optional.of(t));
        assertThat(service.isUnlimited(TENANT)).isTrue();
        assertThat(service.isUnlimited(UUID.randomUUID())).isFalse();
    }
}
