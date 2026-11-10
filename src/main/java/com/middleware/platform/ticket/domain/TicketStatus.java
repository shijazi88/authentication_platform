package com.middleware.platform.ticket.domain;

/**
 * OPEN → the support team owes a reply. IN_PROGRESS → someone is working on it.
 * WAITING_ON_CLIENT → we replied and need the client's answer. RESOLVED → solved;
 * a client reply reopens it. CLOSED → final, no more replies.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_ON_CLIENT,
    RESOLVED,
    CLOSED;

    /** Still needs work from one side or the other. */
    public boolean isActive() {
        return this != RESOLVED && this != CLOSED;
    }
}
