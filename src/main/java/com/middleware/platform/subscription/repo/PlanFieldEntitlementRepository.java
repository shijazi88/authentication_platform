package com.middleware.platform.subscription.repo;

import com.middleware.platform.subscription.domain.PlanFieldEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanFieldEntitlementRepository extends JpaRepository<PlanFieldEntitlement, UUID> {
    List<PlanFieldEntitlement> findByPlanId(UUID planId);
}
