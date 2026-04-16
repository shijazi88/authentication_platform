package com.middleware.platform.iam.repo;

import com.middleware.platform.iam.domain.ApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiCredentialRepository extends JpaRepository<ApiCredential, UUID> {
    Optional<ApiCredential> findByClientId(String clientId);
    List<ApiCredential> findByTenantId(UUID tenantId);
}
