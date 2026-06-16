package com.middleware.platform.wallet.api;

import com.middleware.platform.wallet.domain.WalletEntrySource;
import com.middleware.platform.wallet.dto.TopUpRequest;
import com.middleware.platform.wallet.dto.WalletLedgerEntryResponse;
import com.middleware.platform.wallet.dto.WalletResponse;
import com.middleware.platform.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
