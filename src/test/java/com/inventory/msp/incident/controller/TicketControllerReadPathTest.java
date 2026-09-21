package com.inventory.msp.incident.controller;

import com.inventory.msp.incident.dto.TicketDetailResponse;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.incident.repository.FieldPersonRepository;
import com.inventory.msp.incident.service.IncidentMapper;
import com.inventory.msp.incident.service.TicketService;
import com.inventory.msp.incident.service.TicketWorkflowService;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.UserRole;
import com.inventory.msp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerReadPathTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FieldPersonRepository fieldPersonRepository;

    @Mock
    private IncidentMapper incidentMapper;

    @Mock
    private TicketWorkflowService ticketWorkflowService;

    @InjectMocks
    private TicketController ticketController;

    @Test
    void getTicketDetailReturns200WhenTicketVersionIsNull() {
        AppUser fieldPersonUser = AppUser.builder()
                .id(10L)
                .username("field-user")
                .role(UserRole.FIELD_PERSON)
                .build();

        AppUser supportEngineer = AppUser.builder()
                .id(20L)
                .username("support-user")
                .role(UserRole.SUPPORT_ENGINEER)
                .build();

        FieldPerson fieldPerson = FieldPerson.builder()
                .id(5L)
                .user(fieldPersonUser)
                .build();

        Ticket ticket = Ticket.builder()
                .id(25L)
                .fieldPerson(fieldPerson)
                .raisedByUser(supportEngineer)
                .status(TicketStatus.OPEN)
                .version(null)
                .build();

        TicketDetailResponse detailResponse = TicketDetailResponse.builder()
                .id(25L)
                .status(TicketStatus.OPEN)
                .build();

        when(userRepository.findByUsername("field-user")).thenReturn(Optional.of(fieldPersonUser));
        when(ticketService.getTicket(25L)).thenReturn(ticket);
        when(ticketService.getTicketHistory(25L)).thenReturn(List.of());
        when(ticketWorkflowService.allowedActionsFor(ticket, fieldPersonUser)).thenReturn(List.of("RESOLVED", "REVALIDATION"));
        when(incidentMapper.toTicketDetailResponse(ticket, List.of(), List.of("RESOLVED", "REVALIDATION"))).thenReturn(detailResponse);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                fieldPersonUser.getUsername(), null, "ROLE_FIELD_PERSON");

        ResponseEntity<TicketDetailResponse> response = ticketController.getTicketDetail(25L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(25L, response.getBody().getId());
        assertNull(ticket.getVersion());
    }
}
