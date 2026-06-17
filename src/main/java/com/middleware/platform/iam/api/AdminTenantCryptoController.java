package com.middleware.platform.iam.api;

import com.middleware.platform.iam.domain.TenantEncryptionKey;
import com.middleware.platform.iam.dto.CertificateResponse;
import com.middleware.platform.iam.dto.EncryptionKeyView;
import com.middleware.platform.iam.service.TenantKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin management of a tenant's encryption keys. Authorization follows the
 * /admin/tenants/** matrix (reads: all roles; writes: super/ops).
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/encryption-keys")
@RequiredArgsConstructor
public class AdminTenantCryptoController {

    private final TenantKeyService keyService;

    @GetMapping
    public List<EncryptionKeyView> list(@PathVariable UUID tenantId) {
        return keyService.listKeys(tenantId).stream()
                .map(k -> EncryptionKeyView.of(k, keyService.fingerprint(k)))
                .toList();
    }

    /** The active certificate (with PEM, for download). */
    @GetMapping("/active")
    public CertificateResponse active(@PathVariable UUID tenantId) {
        TenantEncryptionKey key = keyService.getOrCreateActive(tenantId);
        return CertificateResponse.of(key, keyService.fingerprint(key));
    }

    @PostMapping("/rotate")
    public CertificateResponse rotate(@PathVariable UUID tenantId) {
        TenantEncryptionKey key = keyService.rotate(tenantId);
        return CertificateResponse.of(key, keyService.fingerprint(key));
    }

    @PostMapping("/{kid}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID tenantId, @PathVariable String kid) {
        keyService.revoke(tenantId, kid);
    }
}
