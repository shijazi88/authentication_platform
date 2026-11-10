package com.middleware.platform.ticket.dto;

import com.middleware.platform.ticket.domain.TicketStatus;

import java.util.Map;

/**
 * Ticket counts for the admin sidebar badge and filter tabs.
 * {@code needsReply} = OPEN tickets (the client is waiting on the support team).
 */
public record TicketCountsResponse(
        long needsReply,
        long active,
        Map<TicketStatus, Long> byStatus
) {}
