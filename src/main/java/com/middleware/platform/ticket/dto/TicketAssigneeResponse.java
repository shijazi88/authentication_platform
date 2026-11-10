package com.middleware.platform.ticket.dto;

import com.middleware.platform.iam.domain.AdminRole;

/** A team member a ticket can be assigned to. */
public record TicketAssigneeResponse(
        String email,
        String displayName,
        AdminRole role
) {}
