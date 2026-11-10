package com.middleware.platform.ticket.domain;

/** What the ticket is about — picked by the client when opening it. */
public enum TicketCategory {
    VERIFICATION,
    BILLING,
    INTEGRATION,
    ACCOUNT,
    OTHER
}
