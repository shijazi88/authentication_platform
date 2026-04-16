package com.middleware.platform.billing.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Records a billable unit of work. One billing event per successful transaction.
 * Aggregated monthly into invoices.
 */
@Entity
@Table(name = "billing_events", indexes = {
        @Index(name = "idx_billing_tenant_period", columnList = "tenant_id, period"),
        @Index(name = "idx_billing_transaction", columnList = "transaction_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEvent {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Billing period in YYYY-MM form, derived from {@code occurredAt}. */
    @Column(name = "period", nullable = false, length = 7)
    private String period;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static String periodOf(Instant when) {
        return YearMonth.from(when.atZone(java.time.ZoneOffset.UTC)).toString();
    }
}
