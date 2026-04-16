package com.middleware.platform.subscription.repo;

import com.middleware.platform.subscription.domain.PlanEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanEntitlementRepository extends JpaRepository<PlanEntitlement, UUID> {
    List<PlanEntitlement> findByPlanId(UUID planId);
    Optional<PlanEntitlement> findByPlanIdAndOperationId(UUID planId, UUID operationId);
}
