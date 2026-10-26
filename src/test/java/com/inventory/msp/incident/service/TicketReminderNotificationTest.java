package com.inventory.msp.incident.service;

import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.IncidentType;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.incident.repository.TicketHistoryRepository;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class TicketReminderNotificationTest {

    @Test
    void scheduledCycleCreatesOneGroupedNotificationForLoginUser() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TicketHistoryRepository historyRepository = mock(TicketHistoryRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        AppUser loginUser = AppUser.builder().id(38L).username("muzamil").build();
        FieldPerson fieldPerson = FieldPerson.builder().id(14L).name("Field Person").user(loginUser).build();
        LocalDateTime createdAt = LocalDateTime.now().minusHours(43);
        Ticket first = Ticket.builder().id(34L).status(TicketStatus.OPEN).flagged(true)
                .fieldPerson(fieldPerson).createdAt(createdAt).build();
        Ticket second = Ticket.builder().id(35L).status(TicketStatus.OPEN).flagged(true)
                .fieldPerson(fieldPerson).createdAt(createdAt).build();
        when(ticketRepository.findFlaggedTicketIdsToClear(TicketStatus.OPEN)).thenReturn(List.of());
        when(ticketRepository.findTicketsToFlag(any(), any())).thenReturn(List.of());
        when(ticketRepository.findFlaggedTicketsDueForReminder(any(), any())).thenReturn(List.of(first, second));
        when(notificationService.createInAppNotification(
                38L, "muzamil", "TICKET_REMINDER", 34L, "Ticket pending",
                "You have 2 tickets waiting: #34, #35")).thenReturn(null);

        TicketFlagService service = new TicketFlagService(ticketRepository, historyRepository, notificationService);
        ReflectionTestUtils.setField(service, "thresholdHours", 24L);
        ReflectionTestUtils.setField(service, "reminderIntervalHours", 24L);
        ReflectionTestUtils.setField(service, "emailEnabled", false);

        service.processFlagsAndReminders();

        verify(notificationService, times(1)).createInAppNotification(
                38L, "muzamil", "TICKET_REMINDER", 34L, "Ticket pending", "You have 2 tickets waiting: #34, #35");
        verify(ticketRepository, times(2)).saveAll(anyCollection());
    }

    @Test
    void manualReminderReturnsUserFacingResponseAndNotifiesLoginUser() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TicketHistoryRepository historyRepository = mock(TicketHistoryRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        AppUser loginUser = AppUser.builder().id(38L).username("muzamil").build();
        FieldPerson fieldPerson = FieldPerson.builder().id(14L).name("Asha Kumar").user(loginUser).build();
        Ticket ticket = Ticket.builder()
                .id(34L)
                .status(TicketStatus.OPEN)
                .flagged(true)
                .fieldPerson(fieldPerson)
                .incidentType(IncidentType.builder().name("Analytic Cameras out of stock").build())
                .createdAt(LocalDateTime.now().minusHours(43))
                .build();
        when(ticketRepository.findById(34L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(historyRepository.hasNonCreationAction(34L)).thenReturn(false);
        when(notificationService.createInAppNotification(
                eq(38L), eq("muzamil"), eq("TICKET_REMINDER"), eq(34L), eq("Ticket pending"),
                matches("Ticket #34 \\(Analytic Cameras out of stock\\) has had no action for \\d+ hours")))
                .thenReturn(null);

        TicketFlagService service = new TicketFlagService(ticketRepository, historyRepository, notificationService);
        ReflectionTestUtils.setField(service, "emailEnabled", false);

        var result = service.sendManualReminder(34L);

        assertEquals(34L, result.getTicketId());
        assertEquals(1, result.getRemindersSent());
        assertEquals("Reminder sent to Asha Kumar", result.getMessage());
        verify(notificationService).createInAppNotification(
                eq(38L), eq("muzamil"), eq("TICKET_REMINDER"), eq(34L), eq("Ticket pending"),
                matches("Ticket #34 \\(Analytic Cameras out of stock\\) has had no action for \\d+ hours"));
    }

    @Test
    void missingLoginUserSkipsNotificationAndDoesNotFailScheduledCycle() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TicketHistoryRepository historyRepository = mock(TicketHistoryRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        FieldPerson unlinkedFieldPerson = FieldPerson.builder().id(14L).name("No Login").build();
        Ticket ticket = Ticket.builder().id(34L).status(TicketStatus.OPEN).flagged(true)
                .fieldPerson(unlinkedFieldPerson).createdAt(LocalDateTime.now().minusHours(43)).build();
        when(ticketRepository.findFlaggedTicketIdsToClear(TicketStatus.OPEN)).thenReturn(List.of());
        when(ticketRepository.findTicketsToFlag(any(), any())).thenReturn(List.of());
        when(ticketRepository.findFlaggedTicketsDueForReminder(any(), any())).thenReturn(List.of(ticket));

        TicketFlagService service = new TicketFlagService(ticketRepository, historyRepository, notificationService);
        ReflectionTestUtils.setField(service, "thresholdHours", 24L);
        ReflectionTestUtils.setField(service, "reminderIntervalHours", 24L);
        ReflectionTestUtils.setField(service, "emailEnabled", false);

        service.processFlagsAndReminders();

        verify(notificationService, never()).createInAppNotification(any(), any(), any(), any(), any(), any());
        assertEquals(0, ticket.getReminderCount());
        assertEquals(null, ticket.getLastReminderAt());
    }

    @Test
    void failedNotificationInsertDoesNotAbortScheduledCycle() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        TicketHistoryRepository historyRepository = mock(TicketHistoryRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        AppUser loginUser = AppUser.builder().id(38L).username("muzamil").build();
        FieldPerson fieldPerson = FieldPerson.builder().id(14L).name("Field Person").user(loginUser).build();
        Ticket ticket = Ticket.builder().id(34L).status(TicketStatus.OPEN).flagged(true)
                .fieldPerson(fieldPerson).createdAt(LocalDateTime.now().minusHours(43)).build();
        when(ticketRepository.findFlaggedTicketIdsToClear(TicketStatus.OPEN)).thenReturn(List.of());
        when(ticketRepository.findTicketsToFlag(any(), any())).thenReturn(List.of());
        when(ticketRepository.findFlaggedTicketsDueForReminder(any(), any())).thenReturn(List.of(ticket));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("bad notification row"))
                .when(notificationService)
                .createInAppNotification(any(), any(), any(), any(), any(), any());

        TicketFlagService service = new TicketFlagService(ticketRepository, historyRepository, notificationService);
        ReflectionTestUtils.setField(service, "thresholdHours", 24L);
        ReflectionTestUtils.setField(service, "reminderIntervalHours", 24L);
        ReflectionTestUtils.setField(service, "emailEnabled", false);

        service.processFlagsAndReminders();

        assertEquals(0, ticket.getReminderCount());
        assertEquals(null, ticket.getLastReminderAt());
    }
}
