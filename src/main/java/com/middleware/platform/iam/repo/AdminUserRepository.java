package com.middleware.platform.iam.repo;

import com.middleware.platform.iam.domain.AdminRole;
import com.middleware.platform.iam.domain.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Used to guard against removing the last active super-admin. */
    long countByRoleAndActiveTrue(AdminRole role);

    List<AdminUser> findAllByOrderByCreatedAtAsc();
}
