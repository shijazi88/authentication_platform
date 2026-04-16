package com.middleware.platform.transactions.repo;

import com.middleware.platform.transactions.domain.TransactionPayload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionPayloadRepository extends JpaRepository<TransactionPayload, UUID> {
}
