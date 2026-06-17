package com.middleware.platform.gateway.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.tenant.TenantContext;
import com.middleware.platform.iam.domain.TenantEncryptionKey;
import com.middleware.platform.iam.dto.CertificateResponse;
import com.middleware.platform.iam.service.TenantKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Lets an authenticated bank client fetch its active encryption certificate so
 * it can encrypt the verification PII. Public key material only.
 */
@RestController
@RequestMapping("/api/v1/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final TenantKeyService keyService;

    @GetMapping("/certificate")
    public CertificateResponse certificate() {
        UUID tenantId = TenantContext.currentTenantId();
        if (tenantId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHENTICATED, "No tenant in context");
        }
        TenantEncryptionKey key = keyService.getOrCreateActive(tenantId);
        return CertificateResponse.of(key, keyService.fingerprint(key));
    }
}
