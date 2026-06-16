package com.middleware.platform.iam.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.iam.dto.TenantMeResponse;
import com.middleware.platform.iam.security.CurrentTenant;
import com.middleware.platform.iam.service.TenantUserService;
import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.service.SubscriptionService;
import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.service.TransactionService;
import com.middleware.platform.wallet.dto.WalletLedgerEntryResponse;
import com.middleware.platform.wallet.dto.WalletResponse;
import com.middleware.platform.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-facing portal API. Every endpoint is scoped to the authenticated
 * tenant via {@link CurrentTenant} — a tenant can only ever see its own data.
 */
@RestController
@RequestMapping("/portal-api")
@RequiredArgsConstructor
public class TenantPortalController {

    private final TenantUserService tenantUserService;
    private final TransactionService transactionService;
    private final SubscriptionService subscriptionService;
    private final WalletService walletService;

    @GetMapping("/me")
    public TenantMeResponse me() {
        return tenantUserService.me(CurrentTenant.email());
    }

    @GetMapping("/transactions")
    public Page<Transaction> transactions(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionService.listByTenant(CurrentTenant.id(), pr);
    }

    @GetMapping("/transactions/{id}")
    public Transaction transaction(@PathVariable UUID id) {
        Transaction tx = transactionService.get(id);
        if (!tx.getTenantId().equals(CurrentTenant.id())) {
            throw ApplicationException.notFound("Transaction");
        }
        return tx;
    }

    @GetMapping("/subscriptions")
    public List<Subscription> subscriptions() {
        return subscriptionService.listByTenant(CurrentTenant.id());
    }

    @GetMapping("/wallet")
    public WalletResponse wallet() {
        return WalletResponse.from(walletService.getOrCreate(CurrentTenant.id(), "YER"));
    }

    @GetMapping("/wallet/ledger")
    public Page<WalletLedgerEntryResponse> walletLedger(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return walletService.listLedger(CurrentTenant.id(), PageRequest.of(page, Math.min(size, 100)))
                .map(WalletLedgerEntryResponse::from);
    }
}
