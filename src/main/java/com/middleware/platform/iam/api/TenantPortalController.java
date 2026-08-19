package com.middleware.platform.iam.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.iam.dto.ChangePasswordRequest;
import com.middleware.platform.iam.dto.CreateCredentialRequest;
import com.middleware.platform.iam.dto.CredentialResponse;
import com.middleware.platform.iam.dto.CertificateResponse;
import com.middleware.platform.iam.dto.CredentialView;
import com.middleware.platform.iam.dto.SubscriptionDetailResponse;
import com.middleware.platform.iam.dto.TenantMeResponse;
import com.middleware.platform.iam.dto.UpdateProfileRequest;
import com.middleware.platform.iam.domain.TenantEncryptionKey;
import com.middleware.platform.iam.security.CurrentTenant;
import com.middleware.platform.iam.service.TenantKeyService;
import com.middleware.platform.iam.service.TenantPortalService;
import com.middleware.platform.iam.service.TenantUserService;
import com.middleware.platform.device.dto.DeviceResponse;
import com.middleware.platform.device.service.FingerprintDeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.service.SubscriptionService;
import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.domain.TransactionStatus;
import com.middleware.platform.transactions.service.TransactionService;
import com.middleware.platform.wallet.dto.TopUpRequest;
import com.middleware.platform.wallet.dto.TopUpRequestResponse;
import com.middleware.platform.wallet.dto.WalletLedgerEntryResponse;
import com.middleware.platform.wallet.dto.WalletResponse;
import com.middleware.platform.wallet.service.WalletService;
import com.middleware.platform.wallet.service.WalletTopUpRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private final TenantPortalService tenantPortalService;
    private final TenantKeyService tenantKeyService;
    private final FingerprintDeviceService deviceService;
    private final TransactionService transactionService;
    private final SubscriptionService subscriptionService;
    private final WalletService walletService;
    private final WalletTopUpRequestService topUpRequestService;

    @GetMapping("/me")
    public TenantMeResponse me() {
        return tenantUserService.me(CurrentTenant.email());
    }

    /** Self-service: update the signed-in user's own profile (display name). */
    @PutMapping("/me")
    public TenantMeResponse updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return tenantUserService.updateProfile(CurrentTenant.email(), req);
    }

    /** Self-service: change the signed-in user's own password. */
    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        tenantUserService.changePassword(CurrentTenant.email(), req);
    }

    @GetMapping("/transactions")
    public Page<Transaction> transactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) Integer errorCode,
            @RequestParam(required = false) Boolean billable,
            @RequestParam(required = false) Boolean exception,
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        // Inclusive date range: [from 00:00 UTC, to+1day 00:00 UTC). Tenant is
        // always forced to the caller — a client only ever sees its own data.
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        return transactionService.filter(CurrentTenant.id(), status, errorCode, billable, exception, q,
                fromInstant, toInstant, pr);
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

    @GetMapping("/subscriptions/{id}/details")
    public SubscriptionDetailResponse subscriptionDetails(@PathVariable UUID id) {
        return tenantPortalService.subscriptionDetails(CurrentTenant.id(), id);
    }

    // ── API credentials (keys) ────────────────────────────────────────────────

    @GetMapping("/credentials")
    public List<CredentialView> credentials() {
        return tenantPortalService.listCredentials(CurrentTenant.id());
    }

    /** Issues a new key — the plaintext secret is in the response and shown once. */
    @PostMapping("/credentials")
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse createCredential(@Valid @RequestBody CreateCredentialRequest req) {
        return tenantPortalService.issueCredential(CurrentTenant.id(), req);
    }

    @PostMapping("/credentials/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeCredential(@PathVariable UUID id) {
        tenantPortalService.revokeCredential(CurrentTenant.id(), id);
    }

    // ── Encryption certificate (for encrypting verification PII) ──────────────

    @GetMapping("/crypto/certificate")
    public CertificateResponse certificate() {
        TenantEncryptionKey key = tenantKeyService.getOrCreateActive(CurrentTenant.id());
        return CertificateResponse.of(key, tenantKeyService.fingerprint(key));
    }

    @PostMapping("/crypto/certificate/rotate")
    public CertificateResponse rotateCertificate() {
        TenantEncryptionKey key = tenantKeyService.rotate(CurrentTenant.id());
        return CertificateResponse.of(key, tenantKeyService.fingerprint(key));
    }

    /** Read-only list of this tenant's fingerprint devices (managed by admins). */
    @GetMapping("/devices")
    public List<DeviceResponse> devices() {
        return deviceService.list(CurrentTenant.id());
    }

    @GetMapping("/wallet")
    public WalletResponse wallet() {
        return WalletResponse.from(walletService.getOrCreate(CurrentTenant.id(), "YER"));
    }

    @PostMapping("/wallet/topup-request")
    @ResponseStatus(HttpStatus.CREATED)
    public TopUpRequestResponse requestTopUp(@Valid @RequestBody TopUpRequest req) {
        return topUpRequestService.create(CurrentTenant.id(), req.amountMinor(), req.note(), CurrentTenant.email());
    }

    @GetMapping("/wallet/topup-requests")
    public List<TopUpRequestResponse> myTopUpRequests() {
        return topUpRequestService.listByTenant(CurrentTenant.id());
    }

    @GetMapping("/wallet/ledger")
    public Page<WalletLedgerEntryResponse> walletLedger(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return walletService.listLedger(CurrentTenant.id(), PageRequest.of(page, Math.min(size, 100)))
                .map(WalletLedgerEntryResponse::from);
    }
}
