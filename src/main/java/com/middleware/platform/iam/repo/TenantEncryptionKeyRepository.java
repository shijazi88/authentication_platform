package com.middleware.platform.iam.repo;

import com.middleware.platform.iam.domain.EncryptionKeyStatus;
import com.middleware.platform.iam.domain.TenantEncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantEncryptionKeyRepository extends JpaRepository<TenantEncryptionKey, UUID> {

    Optional<TenantEncryptionKey> findByKid(String kid);

    Optional<TenantEncryptionKey> findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, EncryptionKeyStatus status);

    List<TenantEncryptionKey> findByTenantId(UUID tenantId);
}
