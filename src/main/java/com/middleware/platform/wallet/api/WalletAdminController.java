package com.middleware.platform.wallet.api;

import com.middleware.platform.wallet.domain.WalletEntrySource;
import com.middleware.platform.wallet.dto.TopUpDecisionRequest;
import com.middleware.platform.wallet.dto.TopUpRequest;
import com.middleware.platform.wallet.dto.TopUpRequestResponse;
import com.middleware.platform.wallet.dto.WalletLedgerEntryResponse;
import com.middleware.platform.wallet.dto.WalletResponse;
import com.middleware.platform.wallet.service.WalletService;
import com.middleware.platform.wallet.service.WalletTopUpRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin wallet management — viewing balances, recording top-ups and reading the
 * ledger. Financial surface, so restricted to Finance and Super Admin (mirrored
 * in SecurityConfig for /admin/wallets/**).
 */
@RestController
@RequestMapping("/admin/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','FINANCE')")
public class WalletAdminController {

    private final WalletService walletService;
    private final WalletTopUpRequestService topUpRequestService;

    @GetMapping("/{tenantId}")
    public WalletResponse get(@PathVariable UUID tenantId) {
        // getOrCreate so a tenant onboarded after the V11 backfill still resolves.
        return WalletResponse.from(walletService.getOrCreate(tenantId, "YER"));
    }

    @GetMapping("/{tenantId}/ledger")
    public Page<WalletLedgerEntryResponse> ledger(@PathVariable UUID tenantId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return walletService.listLedger(tenantId, PageRequest.of(page, Math.min(size, 100)))
                .map(WalletLedgerEntryResponse::from);
    }

    @PostMapping("/{tenantId}/topup")
    public WalletResponse topUp(@PathVariable UUID tenantId,
                                @Valid @RequestBody TopUpRequest req,
                                Authentication authentication) {
        return WalletResponse.from(walletService.credit(
                tenantId, req.amountMinor(), WalletEntrySource.ADMIN,
                null, req.note(), authentication.getName()));
    }

    // ── Client top-up requests ────────────────────────────────────────────────

    @GetMapping("/{tenantId}/topup-requests")
    public List<TopUpRequestResponse> topUpRequests(@PathVariable UUID tenantId) {
        return topUpRequestService.listByTenant(tenantId);
    }

    @PostMapping("/{tenantId}/topup-requests/{id}/approve")
    public TopUpRequestResponse approveRequest(@PathVariable UUID tenantId, @PathVariable UUID id,
                                               @RequestBody(required = false) TopUpDecisionRequest req,
                                               Authentication authentication) {
        return topUpRequestService.approve(tenantId, id, authentication.getName(),
                req != null ? req.note() : null);
    }

    @PostMapping("/{tenantId}/topup-requests/{id}/reject")
    public TopUpRequestResponse rejectRequest(@PathVariable UUID tenantId, @PathVariable UUID id,
                                              @RequestBody(required = false) TopUpDecisionRequest req,
                                              Authentication authentication) {
        return topUpRequestService.reject(tenantId, id, authentication.getName(),
                req != null ? req.note() : null);
    }
}
