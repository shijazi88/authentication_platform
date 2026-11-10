package com.middleware.platform.ticket.dto;

import com.middleware.platform.ticket.domain.TicketPriority;
import com.middleware.platform.ticket.domain.TicketStatus;
import jakarta.validation.constraints.Size;

/**
 * Admin edit of ticket fields. A null field is left as it is. {@code assignedTo}
 * = an admin email; an empty string unassigns.
 */
public record UpdateTicketRequest(
        TicketStatus status,
        TicketPriority priority,
        @Size(max = 255) String assignedTo
) {}
