package com.middleware.platform.gateway.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.tenant.TenantContext;
import com.middleware.platform.connector.yemenid.YemenIdConnector;
import com.middleware.platform.gateway.dto.VerifyIdentityRequest;
import com.middleware.platform.gateway.dto.VerifyIdentityResponse;
import com.middleware.platform.gateway.orchestrator.VerificationOrchestrator;
import com.middleware.platform.iam.service.PiiCryptoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bank-facing verification endpoints. Authenticated by HTTP Basic with
 * client_id:client_secret (handled by ClientCredentialsAuthFilter).
 *
 * <p>Sensitive fields (national number + biometrics) are accepted either inside
 * an encrypted JWE envelope ({@code encryptedPayload}, preferred) or — while
 * {@code platform.crypto.require-encrypted-pii=false} — in clear (legacy).
 */
@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final VerificationOrchestrator orchestrator;
    private final PiiCryptoService piiCryptoService;

    @Value("${platform.crypto.require-encrypted-pii:false}")
    private boolean requireEncryptedPii;

    @PostMapping("/identity")
    public VerifyIdentityResponse verifyIdentity(@Valid @RequestBody VerifyIdentityRequest req) {
        String nationalNumber;
        Integer fingerPosition;
        String image;

        if (req.isEncrypted()) {
            UUID tenantId = TenantContext.currentTenantId();
            if (tenantId == null) {
                throw new ApplicationException(ErrorCode.UNAUTHENTICATED, "No tenant in context");
            }
            Map<String, Object> pii = piiCryptoService.decrypt(req.encryptedPayload(), tenantId);
            nationalNumber = asString(pii.get("nationalNumber"));
            Object bioObj = pii.get("biometrics");
            if (bioObj instanceof Map<?, ?> bio) {
                fingerPosition = asInteger(bio.get("fingerPosition"));
                image = asString(bio.get("image"));
            } else {
                fingerPosition = null;
                image = null;
            }
        } else {
            if (requireEncryptedPii) {
                throw new ApplicationException(ErrorCode.BAD_REQUEST,
                        "Encrypted payload is required; plaintext requests are no longer accepted");
            }
            nationalNumber = req.nationalNumber();
            fingerPosition = req.biometrics() != null ? req.biometrics().fingerPosition() : null;
            image = req.biometrics() != null ? req.biometrics().image() : null;
        }

        if (nationalNumber == null || nationalNumber.isBlank()) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "nationalNumber is required");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nationalNumber", nationalNumber);
        if (image != null || fingerPosition != null) {
            Map<String, Object> bio = new HashMap<>();
            bio.put("fingerPosition", fingerPosition);
            bio.put("image", image);
            payload.put("biometrics", bio);
        }

        VerificationOrchestrator.OrchestrationResult result = orchestrator.execute(
                YemenIdConnector.KEY,
                YemenIdConnector.OP_VERIFY,
                payload
        );

        return new VerifyIdentityResponse(
                new VerifyIdentityResponse.Transaction(result.transactionId(), result.timestamp(), "OK"),
                result.projected()
        );
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer asInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
