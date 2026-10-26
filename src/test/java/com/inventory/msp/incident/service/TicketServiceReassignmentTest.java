package com.inventory.msp.incident.service;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.exception.InvalidTicketStateTransitionException;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketHistory;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.incident.repository.FieldPersonRepository;
import com.inventory.msp.incident.repository.TicketHistoryRepository;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.ApproachRoadRepository;
import com.inventory.msp.repository.LocationRepository;
import com.inventory.msp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceReassignmentTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketHistoryRepository ticketHistoryRepository;

    @Mock
    private IncidentTypeService incidentTypeService;

    @Mock
    private FieldPersonService fieldPersonService;

    @Mock
    private FieldPersonRepository fieldPersonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ApproachRoadRepository approachRoadRepository;

    @Mock
    private com.inventory.msp.repository.DeviceTypeRepository deviceTypeRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TicketWorkflowService ticketWorkflowService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void reassignTicketAcceptsTodayForAnyActiveFieldPerson() {
        AppUser supportEngineer = AppUser.builder().id(1L).role(UserRole.SUPPORT_ENGINEER).username("support").build();
        AppUser fieldPersonUser = AppUser.builder().id(2L).role(UserRole.FIELD_PERSON).username("field").build();
        FieldPerson fieldPerson = FieldPerson.builder().id(55L).name("Field").active(true).user(fieldPersonUser).build();
        Ticket ticket = Ticket.builder().id(10L).raisedByUser(supportEngineer).status(TicketStatus.OPEN).build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(fieldPersonRepository.findById(55L)).thenReturn(Optional.of(fieldPerson));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.reassignTicketToFieldPerson(10L, 55L, LocalDate.now(), "remarks", supportEngineer);

        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void reassignTicketAcceptsReopenedTicket() {
        AppUser supportEngineer = AppUser.builder().id(1L).role(UserRole.SUPPORT_ENGINEER).username("support").build();
        AppUser fieldPersonUser = AppUser.builder().id(2L).role(UserRole.FIELD_PERSON).username("field").build();
        FieldPerson fieldPerson = FieldPerson.builder().id(55L).name("Field").active(true).user(fieldPersonUser).build();
        Ticket ticket = Ticket.builder().id(10L).raisedByUser(supportEngineer).status(TicketStatus.REOPENED).build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(fieldPersonRepository.findById(55L)).thenReturn(Optional.of(fieldPerson));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.reassignTicketToFieldPerson(10L, 55L, LocalDate.now().plusDays(1), "remarks", supportEngineer);

        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void reassignTicketAcceptsRevalidationTicket() {
        AppUser supportEngineer = AppUser.builder().id(1L).role(UserRole.SUPPORT_ENGINEER).username("support").build();
        AppUser fieldPersonUser = AppUser.builder().id(2L).role(UserRole.FIELD_PERSON).username("field").build();
        FieldPerson fieldPerson = FieldPerson.builder().id(55L).name("Field").active(true).user(fieldPersonUser).build();
        Ticket ticket = Ticket.builder().id(10L).raisedByUser(supportEngineer).status(TicketStatus.REVALIDATION).build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(fieldPersonRepository.findById(55L)).thenReturn(Optional.of(fieldPerson));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.reassignTicketToFieldPerson(10L, 55L, LocalDate.now().plusDays(1), "remarks", supportEngineer);

        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void reassignTicketPersistsStatusOpenToMatchHistory() {
        AppUser supportEngineer = AppUser.builder().id(1L).role(UserRole.SUPPORT_ENGINEER).username("support").build();
        AppUser fieldPersonUser = AppUser.builder().id(2L).role(UserRole.FIELD_PERSON).username("field").build();
        FieldPerson fieldPerson = FieldPerson.builder().id(55L).name("Field").active(true).user(fieldPersonUser).build();
        Ticket ticket = Ticket.builder().id(10L).raisedByUser(supportEngineer).status(TicketStatus.REVALIDATION).build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(fieldPersonRepository.findById(55L)).thenReturn(Optional.of(fieldPerson));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.reassignTicketToFieldPerson(10L, 55L, LocalDate.now().plusDays(1), "remarks", supportEngineer);

        ArgumentCaptor<Ticket> savedTicketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(savedTicketCaptor.capture());
        assertEquals(TicketStatus.OPEN, savedTicketCaptor.getValue().getStatus());

        ArgumentCaptor<TicketHistory> historyCaptor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(ticketHistoryRepository).save(historyCaptor.capture());
        assertEquals("OPEN", historyCaptor.getValue().getToStatus());
    }

    @Test
    void reassignTicketRejectsInactiveFieldPerson() {
        AppUser supportEngineer = AppUser.builder().id(1L).role(UserRole.SUPPORT_ENGINEER).username("support").build();
        FieldPerson inactiveFieldPerson = FieldPerson.builder().id(99L).name("Inactive").active(false).build();
        Ticket ticket = Ticket.builder().id(10L).raisedByUser(supportEngineer).status(TicketStatus.OPEN).build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(fieldPersonRepository.findById(99L)).thenReturn(Optional.of(inactiveFieldPerson));

        assertThrows(InvalidTicketStateTransitionException.class,
                () -> ticketService.reassignTicketToFieldPerson(10L, 99L, LocalDate.now().plusDays(1), "remarks", supportEngineer));
    }
}
