package com.middleware.platform.connector.moi.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface MoiApiCallRepository extends JpaRepository<MoiApiCall, String> {

    Page<MoiApiCall> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<MoiApiCall> findByKindOrderByCreatedAtDesc(MoiApiCall.Kind kind, Pageable pageable);

    /**
     * All MOI calls for a single Sanad transaction, oldest first so the AUTH
     * round-trip (when present) lands before the VERIFY round-trips.
     */
    List<MoiApiCall> findByTransactionIdOrderByCreatedAtAsc(String transactionId);

    @Modifying
    @Query("delete from MoiApiCall c where c.createdAt < :cutoff")
    int deleteOlderThan(Instant cutoff);
}
