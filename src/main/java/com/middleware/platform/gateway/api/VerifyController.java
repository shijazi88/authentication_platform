package com.middleware.platform.gateway.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.common.tenant.TenantContext;
import com.middleware.platform.connector.yemenid.YemenIdConnector;
import com.middleware.platform.gateway.dto.BiometricExceptionReason;
import com.middleware.platform.gateway.dto.VerifyIdentityRequest;
import com.middleware.platform.gateway.dto.VerifyIdentityResponse;
import com.middleware.platform.gateway.orchestrator.VerificationOrchestrator;
import com.middleware.platform.iam.repo.TenantRepository;
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
    private final TenantRepository tenantRepository;

    /** Global enforcement override; per-tenant flag applies on top. */
    @Value("${platform.crypto.require-encrypted-pii:false}")
    private boolean requireEncryptedPii;

    @PostMapping("/identity")
    public VerifyIdentityResponse verifyIdentity(@Valid @RequestBody VerifyIdentityRequest req) {
        String nationalNumber;
        Integer fingerPosition;
        String image;
        boolean isException;
        String excReason;
        String excNote;

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
            Object excObj = pii.get("exception");
            isException = excObj instanceof Map<?, ?>;
            if (excObj instanceof Map<?, ?> ex) {
                excReason = asString(ex.get("reason"));
                excNote = asString(ex.get("note"));
            } else {
                excReason = null;
                excNote = null;
            }
        } else {
            if (encryptionRequired()) {
                throw new ApplicationException(ErrorCode.BAD_REQUEST,
                        "Encrypted payload is required; plaintext requests are no longer accepted");
            }
            nationalNumber = req.nationalNumber();
            fingerPosition = req.biometrics() != null ? req.biometrics().fingerPosition() : null;
            image = req.biometrics() != null ? req.biometrics().image() : null;
            isException = req.exception() != null;
            excReason = req.exception() != null ? req.exception().reason() : null;
            excNote = req.exception() != null ? req.exception().note() : null;
        }

        if (nationalNumber == null || nationalNumber.isBlank()) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "nationalNumber is required");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nationalNumber", nationalNumber);

        if (isException) {
            // Fingerprint exemption: validate the reason, drop any biometrics, and
            // mark the request so the connector calls the backend WITHOUT a fingerprint
            // and does not enforce a biometric match.
            if (!BiometricExceptionReason.isValid(excReason)) {
                throw new ApplicationException(ErrorCode.VALIDATION_FAILED,
                        "exception.reason must be one of HAND_INJURY, AMPUTATION, WORN_PRINTS, MEDICAL, OTHER");
            }
            Map<String, Object> exc = new HashMap<>();
            exc.put("reason", excReason.toUpperCase());
            if (excNote != null && !excNote.isBlank()) exc.put("note", excNote.trim());
            payload.put("exception", exc);
        } else if (image != null || fingerPosition != null) {
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
                new VerifyIdentityResponse.Transaction(result.transactionId(), result.timestamp(),
                        isException ? "EXEMPT" : "OK"),
                result.projected()
        );
    }

    /** Encrypted PII required if the global flag is on or this tenant opted in. */
    private boolean encryptionRequired() {
        if (requireEncryptedPii) return true;
        UUID tenantId = TenantContext.currentTenantId();
        return tenantId != null && tenantRepository.findById(tenantId)
                .map(t -> t.isRequireEncryptedPii())
                .orElse(false);
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
