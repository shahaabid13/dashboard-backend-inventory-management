package com.inventory.msp.incident.service;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.dto.AgeingReportDTO;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.IncidentType;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketPriority;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.incident.repository.TicketRepository.AgeingAssigneeSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgeingReportServiceTest {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");

    @Mock
    private TicketRepository ticketRepository;

    @Test
    void exactlyThreeOpenTicketsDoesNotQualifyAsDefaulter() {
        AgeingReportService service = new AgeingReportService(ticketRepository, 3, 3);
        when(ticketRepository.findAgeingDefaulters(TicketStatus.OPEN, 3, null)).thenReturn(List.of());

        assertEquals(List.of(), service.getAgeingReport());
        verify(ticketRepository).findAgeingDefaulters(TicketStatus.OPEN, 3, null);
    }

    @Test
    void fourOpenTicketsQualifyAndClosedTicketsAreNotListed() {
        AgeingReportService service = new AgeingReportService(ticketRepository, 3, 3);
        AgeingAssigneeSummary summary = summary(42L, "Alex", "alex@example.com", 4);
        FieldPerson assignee = FieldPerson.builder().id(42L).name("Alex").build();
        List<Ticket> fetchedTickets = List.of(
                ticket(1L, assignee, TicketStatus.OPEN, 8),
                ticket(2L, assignee, TicketStatus.OPEN, 5),
                ticket(3L, assignee, TicketStatus.OPEN, 2),
                ticket(4L, assignee, TicketStatus.OPEN, 0),
                ticket(5L, assignee, TicketStatus.RESOLVED, 20));
        when(ticketRepository.findAgeingDefaulters(TicketStatus.OPEN, 3, null))
                .thenReturn(List.of(summary));
        when(ticketRepository.findAgeingTicketsForFieldPersons(TicketStatus.OPEN, List.of(42L)))
                .thenReturn(fetchedTickets);

        AgeingReportDTO report = service.getAgeingReport().get(0);

        assertEquals(42L, report.getUserId());
        assertEquals("alex@example.com", report.getEmail());
        assertEquals(4, report.getOpenTicketCount());
        assertEquals(4, report.getTickets().size());
        assertEquals(8, report.getMaxDaysOpen());
        assertEquals(TicketStatus.OPEN, report.getTickets().get(0).getStatus());
        verify(ticketRepository).findAgeingTicketsForFieldPersons(TicketStatus.OPEN, List.of(42L));
    }

    @Test
    void daysOpenUsesCalendarDaysInIstAndNullEmailIsPreserved() {
        AgeingReportService service = new AgeingReportService(ticketRepository, 3, 3);
        LocalDate ticketDate = LocalDate.now(REPORT_ZONE).minusDays(6);
        AgeingAssigneeSummary summary = summary(7L, "Sam", null, 4);
        FieldPerson assignee = FieldPerson.builder().id(7L).name("Sam").build();
        Ticket ticket = ticket(10L, assignee, TicketStatus.OPEN, 6);
        when(ticketRepository.findAgeingDefaulters(TicketStatus.OPEN, 3, 7L))
                .thenReturn(List.of(summary));
        when(ticketRepository.findAgeingTicketsForFieldPersons(TicketStatus.OPEN, List.of(7L)))
                .thenReturn(List.of(ticket));

        AgeingReportDTO report = service.getAgeingReport(7L);

        assertNull(report.getEmail());
        assertEquals(6, report.getTickets().get(0).getDaysOpen());
        assertEquals(6, report.getMaxDaysOpen());
        assertEquals(ticketDate.atStartOfDay(), report.getTickets().get(0).getCreatedAt());
    }

    @Test
    void userBelowDefaulterThresholdHasNoIndividualReport() {
        AgeingReportService service = new AgeingReportService(ticketRepository, 3, 3);
        when(ticketRepository.findAgeingDefaulters(TicketStatus.OPEN, 3, 99L)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.getAgeingReport(99L));
    }

    private static AgeingAssigneeSummary summary(
            Long userId, String userName, String email, long count) {
        AgeingAssigneeSummary summary = mock(AgeingAssigneeSummary.class);
        when(summary.getUserId()).thenReturn(userId);
        when(summary.getUserName()).thenReturn(userName);
        when(summary.getEmail()).thenReturn(email);
        when(summary.getOpenTicketCount()).thenReturn(count);
        return summary;
    }

    private static Ticket ticket(Long id, FieldPerson assignee, TicketStatus status, long daysAgo) {
        IncidentType incidentType = IncidentType.builder().name("Network Issue").build();
        return Ticket.builder()
                .id(id)
                .fieldPerson(assignee)
                .incidentType(incidentType)
                .status(status)
                .priority(TicketPriority.HIGH)
                .createdAt(LocalDate.now(REPORT_ZONE).minusDays(daysAgo).atStartOfDay())
                .build();
    }
}
