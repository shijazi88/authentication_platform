package com.middleware.platform.unit;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.ticket.domain.SupportTicket;
import com.middleware.platform.ticket.domain.TicketAuthorType;
import com.middleware.platform.ticket.domain.TicketCategory;
import com.middleware.platform.ticket.domain.TicketMessage;
import com.middleware.platform.ticket.domain.TicketPriority;
import com.middleware.platform.ticket.domain.TicketStatus;
import com.middleware.platform.ticket.dto.CreateTicketRequest;
import com.middleware.platform.ticket.dto.StaffReplyRequest;
import com.middleware.platform.ticket.dto.TicketResponse;
import com.middleware.platform.ticket.repo.SupportTicketRepository;
import com.middleware.platform.ticket.repo.TicketMessageRepository;
import com.middleware.platform.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final String CLIENT = "ops@bank.example";
    private static final String AGENT = "agent@motabiq.example";

    private SupportTicketRepository tickets;
    private TicketMessageRepository messages;
    private TicketService service;
    private SupportTicket ticket;

    @BeforeEach
    void setUp() {
        tickets = mock(SupportTicketRepository.class);
        messages = mock(TicketMessageRepository.class);
        TenantRepository tenants = mock(TenantRepository.class);
        service = new TicketService(tickets, messages, tenants);
        ticket = SupportTicket.builder()
                .id(UUID.randomUUID()).ticketNo(1001L).tenantId(TENANT)
                .subject("Verification failing").category(TicketCategory.VERIFICATION)
                .priority(TicketPriority.NORMAL).status(TicketStatus.OPEN)
                .createdBy(CLIENT).lastMessageBy(TicketAuthorType.CLIENT)
                .lastMessageAt(Instant.now()).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(messages.save(any(TicketMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        // The database assigns ticket_no on insert.
        when(tickets.saveAndFlush(any(SupportTicket.class))).thenAnswer(inv -> {
            SupportTicket t = inv.getArgument(0);
            t.setTicketNo(1002L);
            return t;
        });
        when(tenants.findById(TENANT)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("opening a ticket stores it OPEN with NORMAL priority and the first message")
    void createOpensTicket() {
        service.create(TENANT, CLIENT, new CreateTicketRequest(
                "  Need help  ", TicketCategory.BILLING, null, " ", "Top-up not showing"));

        ArgumentCaptor<SupportTicket> saved = ArgumentCaptor.forClass(SupportTicket.class);
        verify(tickets).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(saved.getValue().getPriority()).isEqualTo(TicketPriority.NORMAL);
        assertThat(saved.getValue().getSubject()).isEqualTo("Need help");
        assertThat(saved.getValue().getReference()).isNull();
        verify(messages).save(argThat(m -> m.getAuthorType() == TicketAuthorType.CLIENT
                && m.getBody().equals("Top-up not showing") && !m.isInternal()));
    }

    @Test
    @DisplayName("a public staff reply waits on the client and assigns the ticket to the agent")
    void staffReplyWaitsOnClient() {
        service.staffReply(ticket.getId(), AGENT, new StaffReplyRequest("Please resend the request id", false, null));

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.WAITING_ON_CLIENT);
        assertThat(ticket.getLastMessageBy()).isEqualTo(TicketAuthorType.STAFF);
        assertThat(ticket.getAssignedTo()).isEqualTo(AGENT);
    }

    @Test
    @DisplayName("an internal note changes nothing the client can see")
    void internalNoteKeepsStatus() {
        Instant before = ticket.getLastMessageAt();
        service.staffReply(ticket.getId(), AGENT, new StaffReplyRequest("Checking the logs", true, TicketStatus.RESOLVED));

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getLastMessageBy()).isEqualTo(TicketAuthorType.CLIENT);
        assertThat(ticket.getLastMessageAt()).isEqualTo(before);
    }

    @Test
    @DisplayName("a client reply reopens a resolved ticket")
    void clientReplyReopens() {
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(Instant.now());

        service.clientReply(TENANT, ticket.getId(), CLIENT, "It happened again");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("a closed ticket takes no client replies")
    void closedRejectsClientReply() {
        ticket.setStatus(TicketStatus.CLOSED);

        assertThatThrownBy(() -> service.clientReply(TENANT, ticket.getId(), CLIENT, "Hello?"))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(messages, never()).save(any());
    }

    @Test
    @DisplayName("a tenant cannot read another tenant's ticket")
    void otherTenantSeesNotFound() {
        assertThatThrownBy(() -> service.getForTenant(UUID.randomUUID(), ticket.getId()))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("the client view hides internal notes and the agent's identity")
    void clientViewHidesStaffDetails() {
        ticket.setAssignedTo(AGENT);
        TicketMessage reply = TicketMessage.builder().id(UUID.randomUUID()).ticketId(ticket.getId())
                .authorType(TicketAuthorType.STAFF).authorEmail(AGENT).body("Fixed").createdAt(Instant.now()).build();
        when(messages.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticket.getId())).thenReturn(List.of(reply));

        TicketResponse view = service.getForTenant(TENANT, ticket.getId());

        assertThat(view.assignedTo()).isNull();
        assertThat(view.messages()).singleElement().satisfies(m -> assertThat(m.authorEmail()).isNull());
        verify(messages, never()).findByTicketIdOrderByCreatedAtAsc(any());
    }
}
