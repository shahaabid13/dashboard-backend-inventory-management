package com.inventory.msp.incident.service;

import com.inventory.msp.incident.exception.InvalidTicketStateTransitionException;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketAction;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketWorkflowServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketWorkflowService ticketWorkflowService;

    @Test
    void fieldPersonCanResolveOpenTicketAndAllowedActionsMatch() {
        AppUser fieldPersonUser = AppUser.builder().id(10L).username("fielduser").role(UserRole.FIELD_PERSON).build();
        FieldPerson fieldPerson = FieldPerson.builder().id(5L).name("Field Person").user(fieldPersonUser).build();
        Ticket ticket = Ticket.builder().id(25L).fieldPerson(fieldPerson).status(TicketStatus.OPEN).build();

        assertDoesNotThrow(() -> ticketWorkflowService.validateTransition(ticket, fieldPersonUser, TicketAction.RESOLVED));
        assertEquals(TicketStatus.ASSIGNED_TO_REVIEWER, ticketWorkflowService.resolveStatus(TicketAction.RESOLVED));
        assertEquals(List.of("RESOLVED", "REVALIDATION"), ticketWorkflowService.allowedActionsFor(ticket, fieldPersonUser));
    }

    @Test
    void supportEngineerCanReopenOnlyOnRevalidation() {
        AppUser supportEngineer = AppUser.builder().id(20L).username("support").role(UserRole.SUPPORT_ENGINEER).build();
        AppUser fieldPersonUser = AppUser.builder().id(10L).username("fielduser").role(UserRole.FIELD_PERSON).build();
        FieldPerson fieldPerson = FieldPerson.builder().id(5L).name("Field Person").user(fieldPersonUser).build();
        Ticket ticket = Ticket.builder().id(25L).fieldPerson(fieldPerson).raisedByUser(supportEngineer).status(TicketStatus.REVALIDATION).build();

        assertDoesNotThrow(() -> ticketWorkflowService.validateTransition(ticket, supportEngineer, TicketAction.REOPENED));
        assertEquals(List.of("REOPEN", "SEND_FOR_REVIEW"), ticketWorkflowService.allowedActionsFor(ticket, supportEngineer));
    }

    @Test
    void supportEngineerCanReassignOpenTicketToFutureFieldPerson() {
        AppUser supportEngineer = AppUser.builder().id(20L).username("support").role(UserRole.SUPPORT_ENGINEER).build();
        AppUser fieldPersonUser = AppUser.builder().id(10L).username("fielduser").role(UserRole.FIELD_PERSON).build();
        FieldPerson fieldPerson = FieldPerson.builder().id(5L).name("Field Person").user(fieldPersonUser).active(true).build();
        Ticket ticket = Ticket.builder().id(25L).fieldPerson(fieldPerson).raisedByUser(supportEngineer).status(TicketStatus.OPEN).build();

        assertDoesNotThrow(() -> ticketWorkflowService.validateTransition(ticket, supportEngineer, TicketAction.REASSIGNED));
        assertEquals(List.of("REASSIGN"), ticketWorkflowService.allowedActionsFor(ticket, supportEngineer));
    }

    @Test
    void invalidStatusTransitionThrowsConflict() {
        AppUser supportEngineer = AppUser.builder().id(20L).username("support").role(UserRole.SUPPORT_ENGINEER).build();
        AppUser fieldPersonUser = AppUser.builder().id(10L).username("fielduser").role(UserRole.FIELD_PERSON).build();
        FieldPerson fieldPerson = FieldPerson.builder().id(5L).name("Field Person").user(fieldPersonUser).build();
        Ticket ticket = Ticket.builder().id(25L).fieldPerson(fieldPerson).raisedByUser(supportEngineer).status(TicketStatus.OPEN).build();

        InvalidTicketStateTransitionException ex = assertThrows(
                InvalidTicketStateTransitionException.class,
                () -> ticketWorkflowService.validateTransition(ticket, supportEngineer, TicketAction.REOPENED)
        );

        assertTrue(ex.getMessage().contains("only allowed in"));
    }

    @Test
    void resolveAssigneeUsesReviewerRoleWhenNoTicketReviewerSet() {
        AppUser reviewer = AppUser.builder().id(30L).username("reviewer").role(UserRole.REVIEWER).build();
        when(userRepository.findByRole(UserRole.REVIEWER)).thenReturn(List.of(reviewer));

        AppUser fieldPersonUser = AppUser.builder().id(10L).username("fielduser").role(UserRole.FIELD_PERSON).build();
        FieldPerson fieldPerson = FieldPerson.builder().id(5L).name("Field Person").user(fieldPersonUser).build();
        Ticket ticket = Ticket.builder().id(25L).fieldPerson(fieldPerson).status(TicketStatus.OPEN).build();

        assertEquals(reviewer, ticketWorkflowService.resolveAssignee(ticket, TicketAction.RESOLVED));
    }
}
