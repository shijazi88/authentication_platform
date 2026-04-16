package com.middleware.platform.billing.repo;

import com.middleware.platform.billing.domain.BillingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BillingEventRepository extends JpaRepository<BillingEvent, UUID> {

    boolean existsByTransactionId(UUID transactionId);

    Page<BillingEvent> findByTenantIdAndPeriod(UUID tenantId, String period, Pageable pageable);

    @Query("""
            select new com.middleware.platform.billing.repo.PeriodSummary(
                b.tenantId, b.period, b.currency, sum(b.amountMinor), count(b))
            from BillingEvent b
            where b.tenantId = :tenantId and b.period = :period
            group by b.tenantId, b.period, b.currency
            """)
    List<PeriodSummary> summarize(UUID tenantId, String period);
}
