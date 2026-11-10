package com.middleware.platform.ticket.repo;

import com.middleware.platform.ticket.domain.SupportTicket;
import com.middleware.platform.ticket.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    List<SupportTicket> findByTenantIdOrderByLastMessageAtDesc(UUID tenantId);

    /**
     * Admin ticket listing. A null filter is ignored. {@code activeOnly} drops
     * tickets whose status is in {@code doneStatuses} (resolved/closed).
     * {@code q} matches the subject or the exact ticket number.
     */
    @Query("""
            select t from SupportTicket t
             where (:tenantId is null or t.tenantId = :tenantId)
               and (:status   is null or t.status   = :status)
               and (:activeOnly = false or t.status not in :doneStatuses)
               and (:q is null
                    or lower(t.subject) like lower(concat('%', :q, '%'))
                    or cast(t.ticketNo as string) = :q)
            """)
    Page<SupportTicket> filter(@Param("tenantId") UUID tenantId,
                               @Param("status") TicketStatus status,
                               @Param("activeOnly") boolean activeOnly,
                               @Param("doneStatuses") Collection<TicketStatus> doneStatuses,
                               @Param("q") String q,
                               Pageable pageable);

    @Query("select t.status as status, count(t) as total from SupportTicket t group by t.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        TicketStatus getStatus();
        long getTotal();
    }
}
