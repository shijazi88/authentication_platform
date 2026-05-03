package com.middleware.platform.connector.moi.api;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.connector.moi.MoiTokenService;
import com.middleware.platform.connector.moi.domain.MoiApiCall;
import com.middleware.platform.connector.moi.domain.MoiApiCallRepository;
import com.middleware.platform.connector.moi.domain.MoiCredentials;
import com.middleware.platform.connector.moi.domain.MoiCredentialsRepository;
import com.middleware.platform.connector.moi.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Admin endpoints for managing MOI integration credentials and auditing.
 *
 * <pre>
 * GET  /admin/moi-credentials              - current config (password masked)
 * PUT  /admin/moi-credentials              - update any subset of fields
 * POST /admin/moi-credentials/test         - attempt a fresh token fetch
 * GET  /admin/moi-credentials/calls        - recent audit rows (summary)
 * GET  /admin/moi-credentials/calls/{id}   - full audit row incl. bodies
 * </pre>
 */
@RestController
@RequestMapping("/admin/moi-credentials")
@RequiredArgsConstructor
public class MoiCredentialsController {

    private final MoiCredentialsRepository repo;
    private final MoiApiCallRepository callsRepo;
    private final MoiTokenService tokenService;

    @GetMapping
    public MoiCredentialsResponse get() {
        return MoiCredentialsResponse.from(repo.getSingleton());
    }

    @PutMapping
    @Transactional
    public MoiCredentialsResponse update(@Valid @RequestBody UpdateMoiCredentialsRequest req) {
        MoiCredentials c = repo.getSingleton();
        if (req.authUrl() != null && !req.authUrl().isBlank()) c.setAuthUrl(req.authUrl().trim());
        if (req.verifyUrl() != null && !req.verifyUrl().isBlank()) c.setVerifyUrl(req.verifyUrl().trim());
        if (req.username() != null && !req.username().isBlank()) c.setUsername(req.username().trim());
        if (req.password() != null && !req.password().isEmpty()) c.setPassword(req.password());
        if (req.domain() != null && !req.domain().isBlank()) c.setDomain(req.domain().trim());
        if (req.active() != null) c.setActive(req.active());
        if (req.connectTimeoutMs() != null) c.setConnectTimeoutMs(req.connectTimeoutMs());
        if (req.readTimeoutMs() != null) c.setReadTimeoutMs(req.readTimeoutMs());
        if (req.tokenRefreshSkewSec() != null) c.setTokenRefreshSkewSec(req.tokenRefreshSkewSec());
        c.setUpdatedBy("admin");
        c.setUpdatedAt(Instant.now());
        MoiCredentials saved = repo.save(c);

        // Force next verify to re-login with the new creds.
        tokenService.invalidate();

        return MoiCredentialsResponse.from(saved);
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public TestConnectionResponse test() {
        MoiTokenService.TokenResult r = tokenService.fetchForTest();
        return new TestConnectionResponse(r.ok(), r.expiresAt(), r.errorMessage());
    }

    @GetMapping("/calls")
    public List<MoiApiCallSummary> listCalls(
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String transactionId,
            @RequestParam(defaultValue = "50") int limit) {
        // transactionId wins when provided — used by the transaction detail
        // page to show only the MOI round-trips for that one txn.
        if (transactionId != null && !transactionId.isBlank()) {
            return callsRepo.findByTransactionIdOrderByCreatedAtAsc(transactionId.trim())
                    .stream().map(MoiApiCallSummary::from).toList();
        }
        int capped = Math.max(1, Math.min(limit, 500));
        PageRequest page = PageRequest.of(0, capped);
        Page<MoiApiCall> results = (kind == null || kind.isBlank())
                ? callsRepo.findAllByOrderByCreatedAtDesc(page)
                : callsRepo.findByKindOrderByCreatedAtDesc(parseKind(kind), page);
        return results.getContent().stream().map(MoiApiCallSummary::from).toList();
    }

    @GetMapping("/calls/{id}")
    public MoiApiCallDetail getCall(@PathVariable String id) {
        MoiApiCall call = callsRepo.findById(id)
                .orElseThrow(() -> ApplicationException.notFound("MOI API call " + id));
        return MoiApiCallDetail.from(call);
    }

    private MoiApiCall.Kind parseKind(String raw) {
        try {
            return MoiApiCall.Kind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException(
                    com.middleware.platform.common.error.ErrorCode.VALIDATION_FAILED,
                    "kind must be AUTH or VERIFY");
        }
    }
}
