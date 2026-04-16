package com.middleware.platform.subscription.service;

import com.middleware.platform.catalog.domain.ServiceOperation;
import com.middleware.platform.catalog.service.CatalogService;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.config.CacheConfig;
import com.middleware.platform.subscription.domain.PlanEntitlement;
import com.middleware.platform.subscription.domain.PlanFieldEntitlement;
import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.domain.SubscriptionStatus;
import com.middleware.platform.subscription.dto.ResolvedEntitlement;
import com.middleware.platform.subscription.repo.PlanEntitlementRepository;
import com.middleware.platform.subscription.repo.PlanFieldEntitlementRepository;
import com.middleware.platform.subscription.repo.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves what a tenant is entitled to for a (service, operation) call. This is the
 * single point that the gateway consults before dispatching a request.
 */
@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanEntitlementRepository entitlementRepository;
    private final PlanFieldEntitlementRepository fieldEntitlementRepository;
    private final CatalogService catalogService;

    @Transactional(readOnly = true)
    public ResolvedEntitlement resolve(UUID tenantId, String serviceCode, String operationCode) {
        Subscription sub = subscriptionRepository
                .findFirstByTenantIdAndStatusOrderByStartDateDesc(tenantId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ENTITLEMENT_DENIED,
                        "No active subscription for tenant"));

        // Date window check.
        LocalDate today = LocalDate.now();
        if (sub.getStartDate().isAfter(today)
                || (sub.getEndDate() != null && sub.getEndDate().isBefore(today))) {
            throw new ApplicationException(ErrorCode.ENTITLEMENT_DENIED,
                    "Subscription not active for current date");
        }

        ServiceOperation op = catalogService.getOperation(serviceCode, operationCode);

        PlanEntitlement entitlement = entitlementRepository
                .findByPlanIdAndOperationId(sub.getPlanId(), op.getId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ENTITLEMENT_DENIED,
                        "Plan does not include operation " + serviceCode + "/" + operationCode));

        long unitPrice = entitlement.getUnitPriceOverrideMinor() != null
                ? entitlement.getUnitPriceOverrideMinor()
                : op.getDefaultUnitPriceMinor();

        Set<String> visiblePaths = visibleFieldPathsForPlan(sub.getPlanId());

        return new ResolvedEntitlement(
                sub.getId(),
                sub.getPlanId(),
                op.getId(),
                unitPrice,
                op.getCurrency(),
                entitlement.getMonthlyQuota(),
                entitlement.getRateLimitPerMinute(),
                visiblePaths
        );
    }

    /**
     * The set of response field paths a plan exposes — read on every verify call
     * and almost never written. Cached locally; evicted on plan-entitlement edits
     * via {@code @CacheEvict} in {@code PlanService}.
     */
    @Cacheable(value = CacheConfig.CACHE_PLAN_FIELD_PATHS, key = "#planId")
    public Set<String> visibleFieldPathsForPlan(UUID planId) {
        return fieldEntitlementRepository.findByPlanId(planId).stream()
                .map(PlanFieldEntitlement::getFieldPath)
                .collect(Collectors.toSet());
    }
}
