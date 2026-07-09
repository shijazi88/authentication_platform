package com.middleware.platform.wallet.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.wallet.domain.TopUpRequestStatus;
import com.middleware.platform.wallet.domain.WalletEntrySource;
import com.middleware.platform.wallet.domain.WalletTopUpRequest;
import com.middleware.platform.wallet.dto.TopUpRequestResponse;
import com.middleware.platform.wallet.repo.WalletTopUpRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Client-initiated wallet top-up requests. Clients submit a request; admins
 * approve (which credits the wallet) or reject. Keeps the audit trail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTopUpRequestService {

    private final WalletTopUpRequestRepository requests;
    private final WalletService walletService;

    @Transactional
    public TopUpRequestResponse create(UUID tenantId, long amountMinor, String note, String requestedBy) {
        if (amountMinor <= 0) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Amount must be positive");
        }
        WalletTopUpRequest r = WalletTopUpRequest.builder()
                .tenantId(tenantId)
                .amountMinor(amountMinor)
                .currency("YER")
                .note(blankToNull(note))
                .status(TopUpRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .build();
        requests.save(r);
        log.info("Top-up request {} created: tenant={} amount={}", r.getId(), tenantId, amountMinor);
        return TopUpRequestResponse.from(r);
    }

    @Transactional(readOnly = true)
    public List<TopUpRequestResponse> listByTenant(UUID tenantId) {
        return requests.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(TopUpRequestResponse::from).toList();
    }

    @Transactional
    public TopUpRequestResponse approve(UUID tenantId, UUID requestId, String adminEmail, String note) {
        WalletTopUpRequest r = requirePending(tenantId, requestId);
        // Credit the wallet, then mark approved.
        walletService.credit(tenantId, r.getAmountMinor(), WalletEntrySource.ADMIN,
                "topup-request:" + r.getId(),
                note != null && !note.isBlank() ? note : r.getNote(), adminEmail);
        r.setStatus(TopUpRequestStatus.APPROVED);
        r.setDecidedBy(adminEmail);
        r.setDecidedNote(blankToNull(note));
        r.setDecidedAt(Instant.now());
        return TopUpRequestResponse.from(r);
    }

    @Transactional
    public TopUpRequestResponse reject(UUID tenantId, UUID requestId, String adminEmail, String note) {
        WalletTopUpRequest r = requirePending(tenantId, requestId);
        r.setStatus(TopUpRequestStatus.REJECTED);
        r.setDecidedBy(adminEmail);
        r.setDecidedNote(blankToNull(note));
        r.setDecidedAt(Instant.now());
        return TopUpRequestResponse.from(r);
    }

    private WalletTopUpRequest requirePending(UUID tenantId, UUID requestId) {
        WalletTopUpRequest r = requests.findById(requestId)
                .orElseThrow(() -> ApplicationException.notFound("Top-up request"));
        if (!r.getTenantId().equals(tenantId)) {
            throw ApplicationException.notFound("Top-up request");
        }
        if (r.getStatus() != TopUpRequestStatus.PENDING) {
            throw new ApplicationException(ErrorCode.CONFLICT, "Request has already been decided");
        }
        return r;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
