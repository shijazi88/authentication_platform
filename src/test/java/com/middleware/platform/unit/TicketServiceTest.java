package com.middleware.platform.unit;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.AdminRole;
import com.middleware.platform.iam.domain.AdminUser;
import com.middleware.platform.iam.repo.AdminUserRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.ticket.domain.SupportTicket;
import com.middleware.platform.ticket.domain.TicketAuthorType;
import com.middleware.platform.ticket.domain.TicketCategory;
import com.middleware.platform.ticket.domain.TicketClosedBy;
import com.middleware.platform.ticket.domain.TicketMessage;
import com.middleware.platform.ticket.domain.TicketPriority;
import com.middleware.platform.ticket.domain.TicketStatus;
import com.middleware.platform.ticket.dto.CreateTicketRequest;
import com.middleware.platform.ticket.dto.StaffReplyRequest;
import com.middleware.platform.ticket.dto.TicketResponse;
import com.middleware.platform.ticket.dto.UpdateTicketRequest;
import com.middleware.platform.ticket.repo.SupportTicketRepository;
import com.middleware.platform.ticket.repo.TicketMessageRepository;
import com.middleware.platform.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
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
    private AdminUserRepository adminUsers;
    private TicketService service;
    private SupportTicket ticket;

    @BeforeEach
    void setUp() {
        tickets = mock(SupportTicketRepository.class);
        messages = mock(TicketMessageRepository.class);
        TenantRepository tenants = mock(TenantRepository.class);
        adminUsers = mock(AdminUserRepository.class);
        service = new TicketService(tickets, messages, tenants, adminUsers, 7);
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

    @Test
    @DisplayName("staff marking it resolved waits for the client, with an auto-close date")
    void resolvedWaitsForClient() {
        TicketResponse view = service.staffReply(ticket.getId(), AGENT,
                new StaffReplyRequest("Fixed on our side", false, TicketStatus.RESOLVED));

        assertThat(view.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(view.closedBy()).isNull();
        assertThat(view.autoCloseAt()).isEqualTo(ticket.getResolvedAt().plus(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("the client confirming closes the ticket as confirmed by the client")
    void clientConfirmCloses() {
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(Instant.now());

        service.clientConfirm(TENANT, ticket.getId());

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getClosedBy()).isEqualTo(TicketClosedBy.CLIENT);
        assertThat(ticket.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("a resolved ticket the client never answers closes itself")
    void autoCloseUnconfirmed() {
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(Instant.now().minus(Duration.ofDays(8)));
        when(tickets.findByStatusAndResolvedAtBefore(eq(TicketStatus.RESOLVED), any())).thenReturn(List.of(ticket));

        assertThat(service.autoCloseUnconfirmed()).isEqualTo(1);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getClosedBy()).isEqualTo(TicketClosedBy.AUTO);
    }

    @Test
    @DisplayName("staff closing records it and reopening clears it")
    void staffCloseAndReopen() {
        service.update(ticket.getId(), new UpdateTicketRequest(TicketStatus.CLOSED, null, null));
        assertThat(ticket.getClosedBy()).isEqualTo(TicketClosedBy.STAFF);

        service.update(ticket.getId(), new UpdateTicketRequest(TicketStatus.OPEN, null, null));
        assertThat(ticket.getClosedBy()).isNull();
        assertThat(ticket.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("a ticket can be assigned to an active agent, not to the regulator or an inactive user")
    void assignOnlyToAgents() {
        AdminUser agent = AdminUser.builder().email("finance@motabiq.example").role(AdminRole.FINANCE).active(true).build();
        AdminUser regulator = AdminUser.builder().email("cb@motabiq.example").role(AdminRole.CENTRAL_BANK).active(true).build();
        AdminUser former = AdminUser.builder().email("left@motabiq.example").role(AdminRole.SUPPORT).active(false).build();
        when(adminUsers.findByEmail(agent.getEmail())).thenReturn(Optional.of(agent));
        when(adminUsers.findByEmail(regulator.getEmail())).thenReturn(Optional.of(regulator));
        when(adminUsers.findByEmail(former.getEmail())).thenReturn(Optional.of(former));

        service.update(ticket.getId(), new UpdateTicketRequest(null, null, agent.getEmail()));
        assertThat(ticket.getAssignedTo()).isEqualTo(agent.getEmail());

        for (String email : List.of(regulator.getEmail(), former.getEmail(), "nobody@motabiq.example")) {
            assertThatThrownBy(() -> service.update(ticket.getId(), new UpdateTicketRequest(null, null, email)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
        }

        service.update(ticket.getId(), new UpdateTicketRequest(null, null, ""));
        assertThat(ticket.getAssignedTo()).isNull();
    }
}
