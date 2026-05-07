package com.middleware.platform.iam.repo;

import com.middleware.platform.iam.domain.Tenant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByCode(String code);
    boolean existsByCode(String code);

    @Query("""
            SELECT t FROM Tenant t
            WHERE LOWER(t.code) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.legalName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(t.contactEmail) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY t.legalName ASC
            """)
    List<Tenant> search(@Param("q") String q, Pageable pageable);
}
