package com.middleware.platform.billing.service;

import com.middleware.platform.billing.domain.BillingEvent;
import com.middleware.platform.billing.repo.BillingEventRepository;
import com.middleware.platform.billing.repo.PeriodSummary;
import com.middleware.platform.transactions.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingEventRepository repository;

    /**
     * Idempotently record a billing event for a successful, billable transaction.
     *
     * <p>{@code REQUIRES_NEW} is critical here: this method is invoked from a
     * {@code @TransactionalEventListener(AFTER_COMMIT)} hook, which fires after the
     * originating transaction has already committed. Without REQUIRES_NEW the
     * persist action gets scheduled into a phantom synchronization that never
     * flushes, and the row silently disappears. REQUIRES_NEW forces a brand-new
     * transaction with a fresh EntityManager that commits independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFromTransaction(TransactionCompletedEvent event) {
        log.debug("recordFromTransaction tx={} billable={} unitPrice={} currency={}",
                event.transactionId(), event.billable(), event.unitPriceMinor(), event.currency());
        if (!event.billable()) {
            log.debug("Skipping non-billable event for tx={}", event.transactionId());
            return;
        }
        if (repository.existsByTransactionId(event.transactionId())) {
            log.debug("Billing event already exists for tx={}", event.transactionId());
            return;
        }
        try {
            BillingEvent be = BillingEvent.builder()
                    .transactionId(event.transactionId())
                    .tenantId(event.tenantId())
                    .subscriptionId(event.subscriptionId())
                    .serviceId(event.serviceId())
                    .operationId(event.operationId())
                    .unitPriceMinor(event.unitPriceMinor())
                    .amountMinor(event.unitPriceMinor()) // 1 unit per transaction
                    .currency(event.currency())
                    .period(BillingEvent.periodOf(event.occurredAt()))
                    .occurredAt(event.occurredAt())
                    .build();
            BillingEvent saved = repository.save(be);
            log.info("Billing event persisted id={} tx={} amount={} {}",
                    saved.getId(), saved.getTransactionId(), saved.getAmountMinor(), saved.getCurrency());
        } catch (Exception ex) {
            log.error("Failed to persist billing event for tx={}", event.transactionId(), ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<BillingEvent> listByTenantAndPeriod(UUID tenantId, String period, Pageable pageable) {
        return repository.findByTenantIdAndPeriod(tenantId, period, pageable);
    }

    @Transactional(readOnly = true)
    public List<PeriodSummary> summarize(UUID tenantId, String period) {
        return repository.summarize(tenantId, period);
    }
}
