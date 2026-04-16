package com.middleware.platform.subscription.repo;

import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByTenantId(UUID tenantId);
    Optional<Subscription> findFirstByTenantIdAndStatusOrderByStartDateDesc(UUID tenantId, SubscriptionStatus status);
}
