package com.middleware.platform.ticket.domain;

/** Who closed a ticket: the client confirming it is solved, the support team, or the auto-close job. */
public enum TicketClosedBy {
    CLIENT,
    STAFF,
    AUTO
}
