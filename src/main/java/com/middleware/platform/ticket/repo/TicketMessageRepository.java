package com.middleware.platform.ticket.repo;

import com.middleware.platform.ticket.domain.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {
    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
    List<TicketMessage> findByTicketIdAndInternalFalseOrderByCreatedAtAsc(UUID ticketId);
}
