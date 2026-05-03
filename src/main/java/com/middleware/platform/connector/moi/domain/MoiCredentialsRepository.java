package com.middleware.platform.connector.moi.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MoiCredentialsRepository extends JpaRepository<MoiCredentials, Long> {

    /** The single config row. Throws if missing (Flyway V9 seeds it). */
    default MoiCredentials getSingleton() {
        return findById(MoiCredentials.SINGLETON_ID).orElseThrow(() ->
                new IllegalStateException("moi_credentials singleton row (id=1) not found — has Flyway V9 run?"));
    }
}
