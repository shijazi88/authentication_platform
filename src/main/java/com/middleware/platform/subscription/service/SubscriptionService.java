package com.middleware.platform.subscription.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.domain.SubscriptionStatus;
import com.middleware.platform.subscription.dto.CreateSubscriptionRequest;
import com.middleware.platform.subscription.repo.PlanRepository;
import com.middleware.platform.subscription.repo.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final TenantRepository tenantRepository;

    /**
     * Creates a new ACTIVE subscription for a tenant. If the tenant already has
     * an ACTIVE subscription, that previous one is auto-expired in the same
     * transaction (its end_date is set to the day before the new start_date and
     * its status flips to EXPIRED). This guarantees the tenant always has at
     * most one ACTIVE subscription at any moment, which is what the entitlement
     * resolver assumes.
     */
    @Transactional
    public Subscription create(CreateSubscriptionRequest req) {
        tenantRepository.findById(req.tenantId())
                .orElseThrow(() -> ApplicationException.notFound("Tenant"));
        planRepository.findById(req.planId())
                .orElseThrow(() -> ApplicationException.notFound("Plan"));

        subscriptionRepository
                .findFirstByTenantIdAndStatusOrderByStartDateDesc(req.tenantId(), SubscriptionStatus.ACTIVE)
                .ifPresent(prev -> {
                    LocalDate newStart = req.startDate();
                    LocalDate prevEnd = newStart.minusDays(1);
                    prev.setStatus(SubscriptionStatus.EXPIRED);
                    prev.setEndDate(prevEnd);
                    log.info("Auto-expiring previous active subscription {} for tenant {} (end_date={}, replaced by plan {})",
                            prev.getId(), req.tenantId(), prevEnd, req.planId());
                });

        Subscription sub = Subscription.builder()
                .tenantId(req.tenantId())
                .planId(req.planId())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(SubscriptionStatus.ACTIVE)
                .build();
        return subscriptionRepository.save(sub);
    }

    @Transactional(readOnly = true)
    public List<Subscription> listByTenant(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Subscription setStatus(UUID id, SubscriptionStatus status) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> ApplicationException.notFound("Subscription"));
        s.setStatus(status);
        return s;
    }
}
