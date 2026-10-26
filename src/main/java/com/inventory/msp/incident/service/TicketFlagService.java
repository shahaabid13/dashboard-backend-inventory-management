package com.inventory.msp.incident.service;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.dto.FlaggedTicketDTO;
import com.inventory.msp.incident.dto.MyPendingTicketDTO;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketQueueRules;
import com.inventory.msp.incident.model.TicketStatus;
import com.inventory.msp.incident.repository.TicketHistoryRepository;
import com.inventory.msp.incident.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketFlagService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final NotificationService notificationService;

    @Value("${cims.flag.threshold-hours:24}")
    private long thresholdHours;

    @Value("${cims.flag.reminder-interval-hours:24}")
    private long reminderIntervalHours;

    @Value("${cims.flag.email-enabled:false}")
    private boolean emailEnabled;

    @PostConstruct
    void validateConfiguration() {
        if (thresholdHours <= 0 || reminderIntervalHours <= 0) {
            throw new IllegalArgumentException("CIMS flag and reminder intervals must be greater than zero");
        }
    }

    @Transactional("transactionManager")
    public void processFlagsAndReminders() {
        LocalDateTime now = LocalDateTime.now(TicketQueueRules.REPORT_ZONE);
        clearIneligibleFlags();
        flagEligibleTickets(now);
        sendDueReminders(now);
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<FlaggedTicketDTO> getFlaggedTickets(Long userId) {
        LocalDateTime now = LocalDateTime.now(TicketQueueRules.REPORT_ZONE);
        return ticketRepository.findFlaggedTickets(TicketQueueRules.DEFAULTER_STATUS, userId).stream()
                .map(ticket -> toDto(ticket, now))
                .toList();
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<MyPendingTicketDTO> getMyPendingTickets(Long userId) {
        LocalDateTime now = LocalDateTime.now(TicketQueueRules.REPORT_ZONE);
        return ticketRepository.findMyPendingTickets(userId, TicketQueueRules.DEFAULTER_STATUS).stream()
                .map(ticket -> MyPendingTicketDTO.builder()
                        .ticketId(ticket.getTicketId())
                        .title(ticket.getTitle())
                        .location(ticket.getLocation())
                        .hoursPending(ticket.getTicketStartAt() == null ? 0
                                : Math.max(0, Duration.between(ticket.getTicketStartAt(), now).toHours()))
                        .flagged(ticket.getFlagged())
                        .build())
                .toList();
    }

    @Transactional("transactionManager")
    public FlaggedTicketDTO sendManualReminder(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found: " + ticketId));
        if (!ticket.isFlagged()
                || !TicketQueueRules.DEFAULTER_STATUSES.contains(ticket.getStatus())
                || ticketHistoryRepository.hasNonCreationAction(ticketId)) {
            throw new NotFoundException("Flagged open ticket not found: " + ticketId);
        }
        LocalDateTime now = LocalDateTime.now(TicketQueueRules.REPORT_ZONE);
        String recipient = recipientUsername(ticket);
        if (recipient == null) {
            log.warn("Skipping manual reminder for ticket {}: assignee has no linked login username", ticketId);
            return toDto(ticket, now).toBuilder()
                    .ticketId(ticket.getId())
                    .remindersSent(0)
                    .message("Reminder not sent: assignee has no linked login username")
                    .build();
        }
        boolean notificationCreated = remind(List.of(ticket), now, false);
        if (!notificationCreated) {
            return toDto(ticket, now).toBuilder()
                    .ticketId(ticket.getId())
                    .remindersSent(0)
                    .message("Reminder could not be saved; please retry")
                    .build();
        }
        ticket.setLastReminderAt(now);
        ticket.setReminderCount(ticket.getReminderCount() + 1);
        ticketRepository.save(ticket);
        return toDto(ticket, now).toBuilder()
                .ticketId(ticket.getId())
                .remindersSent(1)
                .message("Reminder sent to " + ticket.getFieldPerson().getName())
                .build();
    }

    static boolean shouldFlag(TicketStatus status, LocalDateTime createdAt, boolean actionTaken,
                              LocalDateTime now, long thresholdHours) {
        return TicketQueueRules.DEFAULTER_STATUSES.contains(status)
                && !actionTaken
                && createdAt != null
                && createdAt.isBefore(now.minusHours(thresholdHours));
    }

    private void clearIneligibleFlags() {
        List<Long> ids = ticketRepository.findFlaggedTicketIdsToClear(TicketQueueRules.DEFAULTER_STATUS);
        if (!ids.isEmpty()) {
            ticketRepository.clearFlags(ids);
        }
    }

    private void flagEligibleTickets(LocalDateTime now) {
        LocalDateTime createdBefore = now.minusHours(thresholdHours);
        List<Ticket> eligible = ticketRepository.findTicketsToFlag(
                TicketQueueRules.DEFAULTER_STATUS, createdBefore);
        for (Ticket ticket : eligible) {
            if (shouldFlag(ticket.getStatus(), TicketQueueRules.ticketStartTime(ticket), false, now, thresholdHours)) {
                ticket.setFlagged(true);
                ticket.setFlaggedAt(now);
            }
        }
        ticketRepository.saveAll(eligible);
    }

    private void sendDueReminders(LocalDateTime now) {
        List<Ticket> due = ticketRepository.findFlaggedTicketsDueForReminder(
                TicketQueueRules.DEFAULTER_STATUS, now.minusHours(reminderIntervalHours));
        Map<String, List<Ticket>> byUsername = new LinkedHashMap<>();
        for (Ticket ticket : due) {
            String username = recipientUsername(ticket);
            if (username == null) {
                log.warn("Skipping reminder for ticket {}: assignee has no linked login username", ticket.getId());
                continue;
            }
            byUsername.computeIfAbsent(username, ignored -> new ArrayList<>())
                    .add(ticket);
        }

        for (Map.Entry<String, List<Ticket>> entry : byUsername.entrySet()) {
            List<Ticket> userTickets = entry.getValue();
            if (remind(userTickets, now, true)) {
                for (Ticket ticket : userTickets) {
                    ticket.setLastReminderAt(now);
                    ticket.setReminderCount(ticket.getReminderCount() + 1);
                }
            }
        }
        ticketRepository.saveAll(due);
    }

    private boolean remind(List<Ticket> tickets, LocalDateTime now, boolean grouped) {
        if (tickets.isEmpty()) {
            return false;
        }
        Ticket first = tickets.get(0);
        String username = recipientUsername(first);
        if (username == null) {
            log.warn("Skipping reminder for ticket {}: assignee has no linked login username", first.getId());
            return false;
        }
        String content;
        Long userId = first.getFieldPerson().getUser().getId();
        if (userId == null || userId <= 0) {
            log.warn("Skipping reminder for ticket {}: assignee login user has no database id", first.getId());
            return false;
        }
        if (grouped) {
            content = "You have " + tickets.size() + " tickets waiting: "
                    + tickets.stream().map(ticket -> "#" + ticket.getId())
                    .collect(java.util.stream.Collectors.joining(", "));
        } else {
            long hours = Math.max(0, Duration.between(TicketQueueRules.ticketStartTime(first), now).toHours());
            String title = first.getIncidentType() == null ? "Ticket" : first.getIncidentType().getName();
            content = "Ticket #" + first.getId() + " (" + title + ") has had no action for " + hours + " hours";
        }
        try {
            notificationService.createInAppNotification(
                    userId, username, "TICKET_REMINDER", first.getId(), "Ticket pending", content);
        } catch (DataAccessException exception) {
            log.error("Could not save ticket reminder notification for user {} and ticket {}",
                    username, first.getId(), exception);
            return false;
        }
        if (emailEnabled) {
            notificationService.sendFlagReminderEmail(username, "Ticket pending", content);
        }
        return true;
    }

    private String recipientUsername(Ticket ticket) {
        FieldPerson assignee = ticket.getFieldPerson();
        if (assignee == null || assignee.getUser() == null) {
            return null;
        }
        String username = assignee.getUser().getUsername();
        return username == null || username.isBlank() ? null : username;
    }

    private FlaggedTicketDTO toDto(Ticket ticket, LocalDateTime now) {
        LocalDateTime ticketStartTime = TicketQueueRules.ticketStartTime(ticket);
        long hoursPending = ticketStartTime == null
                ? 0
                : Math.max(0, Duration.between(ticketStartTime, now).toHours());
        return FlaggedTicketDTO.builder()
                .id(ticket.getId())
                .ticketId(ticket.getId())
                .title(ticket.getIncidentType() == null ? null : ticket.getIncidentType().getName())
                .assignee(ticket.getFieldPerson() == null ? null : ticket.getFieldPerson().getName())
                .createdAt(ticketStartTime)
                .hoursPending(hoursPending)
                .reminderCount(ticket.getReminderCount())
                .build();
    }

}
