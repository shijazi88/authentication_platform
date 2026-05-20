package com.middleware.platform.subscription.repo;

import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByTenantId(UUID tenantId);
    Optional<Subscription> findFirstByTenantIdAndStatusOrderByStartDateDesc(UUID tenantId, SubscriptionStatus status);

    List<Subscription> findByPlanId(UUID planId);

    /**
     * (planId, totalCount, activeCount) across all plans. Used by the
     * /admin/plans/subscriber-counts endpoint to populate the index
     * page's "Subscribers" column in a single round-trip (avoids N+1).
     */
    @Query("""
            select s.planId,
                   count(s),
                   sum(case when s.status = com.middleware.platform.subscription.domain.SubscriptionStatus.ACTIVE then 1 else 0 end)
            from Subscription s
            group by s.planId
            """)
    List<Object[]> countByPlanRaw();
}
