package com.middleware.platform.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actorId, String actorEmail, String action,
                       String targetType, String targetId, String detail, String ipAddress) {
        try {
            repository.save(AuditLog.builder()
                    .actorId(actorId)
                    .actorEmail(actorEmail)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detail != null && detail.length() > 2048 ? detail.substring(0, 2048) : detail)
                    .ipAddress(ipAddress)
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to write audit log: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listByTarget(String targetType, String targetId, Pageable pageable) {
        return repository.findByTargetTypeAndTargetId(targetType, targetId, pageable);
    }
}
