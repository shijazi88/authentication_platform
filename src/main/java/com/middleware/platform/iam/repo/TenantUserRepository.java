package com.middleware.platform.iam.repo;

import com.middleware.platform.iam.domain.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {
    Optional<TenantUser> findByEmail(String email);
    boolean existsByEmail(String email);
    List<TenantUser> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);
}
