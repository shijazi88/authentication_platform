package com.middleware.platform.subscription.repo;

import com.middleware.platform.subscription.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByCode(String code);
    boolean existsByCode(String code);
}
