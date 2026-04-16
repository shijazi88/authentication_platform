package com.middleware.platform.unit;

import com.middleware.platform.catalog.domain.ServiceOperation;
import com.middleware.platform.catalog.service.CatalogService;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.subscription.domain.PlanEntitlement;
import com.middleware.platform.subscription.domain.PlanFieldEntitlement;
import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.domain.SubscriptionStatus;
import com.middleware.platform.subscription.dto.ResolvedEntitlement;
import com.middleware.platform.subscription.repo.PlanEntitlementRepository;
import com.middleware.platform.subscription.repo.PlanFieldEntitlementRepository;
import com.middleware.platform.subscription.repo.SubscriptionRepository;
import com.middleware.platform.subscription.service.EntitlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock PlanEntitlementRepository planEntitlementRepository;
    @Mock PlanFieldEntitlementRepository planFieldEntitlementRepository;
    @Mock CatalogService catalogService;

    @InjectMocks EntitlementService entitlementService;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID OP = UUID.randomUUID();
    private static final UUID SUB = UUID.randomUUID();

    @Test
    @DisplayName("resolve → success when active subscription + plan entitlement exist")
    void resolve_success() {
        Subscription sub = Subscription.builder()
                .id(SUB).tenantId(TENANT).planId(PLAN)
                .startDate(LocalDate.now().minusDays(1))
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findFirstByTenantIdAndStatusOrderByStartDateDesc(TENANT, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));

        ServiceOperation op = ServiceOperation.builder()
                .id(OP).code("verify").defaultUnitPriceMinor(100L).currency("YER").active(true).build();
        when(catalogService.getOperation("YEMEN_ID", "verify")).thenReturn(op);

        PlanEntitlement pe = PlanEntitlement.builder()
                .id(UUID.randomUUID()).planId(PLAN).operationId(OP)
                .unitPriceOverrideMinor(50L).monthlyQuota(1000L).rateLimitPerMinute(60)
                .build();
        when(planEntitlementRepository.findByPlanIdAndOperationId(PLAN, OP))
                .thenReturn(Optional.of(pe));

        when(planFieldEntitlementRepository.findByPlanId(PLAN))
                .thenReturn(List.of(
                        PlanFieldEntitlement.builder().planId(PLAN).fieldPath("verification.status").build()));

        ResolvedEntitlement result = entitlementService.resolve(TENANT, "YEMEN_ID", "verify");

        assertThat(result.subscriptionId()).isEqualTo(SUB);
        assertThat(result.unitPriceMinor()).isEqualTo(50L);
        assertThat(result.visibleFieldPaths()).containsExactly("verification.status");
    }

    @Test
    @DisplayName("resolve → ENTITLEMENT_DENIED when no active subscription")
    void resolve_noSubscription() {
        when(subscriptionRepository.findFirstByTenantIdAndStatusOrderByStartDateDesc(TENANT, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> entitlementService.resolve(TENANT, "YEMEN_ID", "verify"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ENTITLEMENT_DENIED));
    }

    @Test
    @DisplayName("resolve → ENTITLEMENT_DENIED when plan does not include operation")
    void resolve_operationNotInPlan() {
        Subscription sub = Subscription.builder()
                .id(SUB).tenantId(TENANT).planId(PLAN)
                .startDate(LocalDate.now().minusDays(1))
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findFirstByTenantIdAndStatusOrderByStartDateDesc(TENANT, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));

        ServiceOperation op = ServiceOperation.builder()
                .id(OP).code("verify").defaultUnitPriceMinor(100L).currency("YER").active(true).build();
        when(catalogService.getOperation("YEMEN_ID", "verify")).thenReturn(op);

        when(planEntitlementRepository.findByPlanIdAndOperationId(PLAN, OP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> entitlementService.resolve(TENANT, "YEMEN_ID", "verify"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ENTITLEMENT_DENIED));
    }

    @Test
    @DisplayName("resolve → ENTITLEMENT_DENIED when subscription is outside date window")
    void resolve_subscriptionExpired() {
        Subscription sub = Subscription.builder()
                .id(SUB).tenantId(TENANT).planId(PLAN)
                .startDate(LocalDate.now().plusDays(5)) // starts in the future
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findFirstByTenantIdAndStatusOrderByStartDateDesc(TENANT, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> entitlementService.resolve(TENANT, "YEMEN_ID", "verify"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ENTITLEMENT_DENIED));
    }
}
