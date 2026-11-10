package com.middleware.platform.ticket.dto;

import com.middleware.platform.ticket.domain.SupportTicket;
import com.middleware.platform.ticket.domain.TicketAuthorType;
import com.middleware.platform.ticket.domain.TicketCategory;
import com.middleware.platform.ticket.domain.TicketPriority;
import com.middleware.platform.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A ticket. {@code messages} is filled on the detail endpoints and null in
 * lists. {@code tenantName} is filled for the admin side only;
 * {@code assignedTo} is never sent to the client.
 */
public record TicketResponse(
        UUID id,
        long ticketNo,
        UUID tenantId,
        String tenantName,
        String subject,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        String reference,
        String createdBy,
        String assignedTo,
        Instant lastMessageAt,
        TicketAuthorType lastMessageBy,
        Instant createdAt,
        Instant resolvedAt,
        Instant closedAt,
        List<TicketMessageResponse> messages
) {
    public static TicketResponse forStaff(SupportTicket t, String tenantName, List<TicketMessageResponse> messages) {
        return new TicketResponse(t.getId(), t.getTicketNo(), t.getTenantId(), tenantName,
                t.getSubject(), t.getCategory(), t.getPriority(), t.getStatus(), t.getReference(),
                t.getCreatedBy(), t.getAssignedTo(), t.getLastMessageAt(), t.getLastMessageBy(),
                t.getCreatedAt(), t.getResolvedAt(), t.getClosedAt(), messages);
    }

    public static TicketResponse forClient(SupportTicket t, List<TicketMessageResponse> messages) {
        return new TicketResponse(t.getId(), t.getTicketNo(), t.getTenantId(), null,
                t.getSubject(), t.getCategory(), t.getPriority(), t.getStatus(), t.getReference(),
                t.getCreatedBy(), null, t.getLastMessageAt(), t.getLastMessageBy(),
                t.getCreatedAt(), t.getResolvedAt(), t.getClosedAt(), messages);
    }
}
