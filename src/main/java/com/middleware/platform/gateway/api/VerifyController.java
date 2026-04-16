package com.middleware.platform.gateway.api;

import com.middleware.platform.connector.yemenid.YemenIdConnector;
import com.middleware.platform.gateway.dto.VerifyIdentityRequest;
import com.middleware.platform.gateway.dto.VerifyIdentityResponse;
import com.middleware.platform.gateway.orchestrator.VerificationOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bank-facing verification endpoints. Authenticated by HTTP Basic with
 * client_id:client_secret (handled by ClientCredentialsAuthFilter).
 */
@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final VerificationOrchestrator orchestrator;

    @PostMapping("/identity")
    public VerifyIdentityResponse verifyIdentity(@Valid @RequestBody VerifyIdentityRequest req) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nationalNumber", req.nationalNumber());
        if (req.biometrics() != null) {
            Map<String, Object> bio = new HashMap<>();
            bio.put("fingerPosition", req.biometrics().fingerPosition());
            bio.put("image", req.biometrics().image());
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
}
