package com.middleware.platform.billing.listener;

import com.middleware.platform.billing.service.BillingService;
import com.middleware.platform.transactions.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionListener {

    private final BillingService billingService;

    /**
     * Bridges the transaction module's domain event to billing — runs in a fresh
     * transaction after the originating transaction commits, so a billing failure
     * never rolls back a verification.
     *
     * <p>{@code fallbackExecution=true} ensures the listener still fires if the
     * publisher happens to call {@code publishEvent} outside a transaction —
     * useful for non-orchestrator code paths and tests.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        log.debug("Billing listener received event for tx={}", event.transactionId());
        billingService.recordFromTransaction(event);
    }
}
