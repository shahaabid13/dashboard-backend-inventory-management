package com.inventory.msp.incident.service;

import com.inventory.msp.exception.NotFoundException;
import com.inventory.msp.incident.dto.AgeingReportDTO;
import com.inventory.msp.incident.dto.AgeingTicketDTO;
import com.inventory.msp.incident.dto.TopDefaulterDTO;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketQueueRules;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.incident.repository.TicketRepository.AgeingAssigneeSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AgeingReportService {

    private final TicketRepository ticketRepository;
    private final long threshold;
    private final int topLimit;

    public AgeingReportService(
            TicketRepository ticketRepository,
            @Value("${cims.ageing.threshold:3}") long threshold,
            @Value("${cims.ageing.top-limit:3}") int topLimit) {
        if (threshold < 0) {
            throw new IllegalArgumentException("cims.ageing.threshold must not be negative");
        }
        if (topLimit < 1) {
            throw new IllegalArgumentException("cims.ageing.top-limit must be greater than zero");
        }
        this.ticketRepository = ticketRepository;
        this.threshold = threshold;
        this.topLimit = topLimit;
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<AgeingReportDTO> getAgeingReport() {
        return buildReports(findDefaulters(null));
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public AgeingReportDTO getAgeingReport(Long userId) {
        List<AgeingAssigneeSummary> defaulters = findDefaulters(userId);
        if (defaulters.isEmpty()) {
            throw new NotFoundException("No ageing report found for user: " + userId);
        }
        return buildReports(defaulters).get(0);
    }

    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<TopDefaulterDTO> getTopDefaulters() {
        return findDefaulters(null).stream()
                .sorted(defaulterOrder())
                .limit(topLimit)
                .map(summary -> TopDefaulterDTO.builder()
                        .userId(summary.getUserId())
                        .userName(summary.getUserName())
                        .email(summary.getEmail())
                        .openTicketCount(summary.getOpenTicketCount())
                        .oldestTicketDays(daysOpen(summary.getOldestCreatedAt()))
                        .build())
                .toList();
    }

    private List<AgeingAssigneeSummary> findDefaulters(Long userId) {
        return ticketRepository.findAgeingDefaulters(
                TicketQueueRules.DEFAULTER_STATUS, threshold, userId);
    }

    private List<AgeingReportDTO> buildReports(List<AgeingAssigneeSummary> defaulters) {
        if (defaulters.isEmpty()) {
            return List.of();
        }

        List<Long> fieldPersonIds = defaulters.stream()
                .map(AgeingAssigneeSummary::getUserId)
                .toList();
        List<Ticket> tickets = ticketRepository.findAgeingTicketsForFieldPersons(
                TicketQueueRules.DEFAULTER_STATUS, fieldPersonIds);
        Map<Long, List<Ticket>> ticketsByFieldPerson = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            if (!TicketQueueRules.DEFAULTER_STATUSES.contains(ticket.getStatus())) {
                continue;
            }
            ticketsByFieldPerson.computeIfAbsent(ticket.getFieldPerson().getId(), ignored -> new ArrayList<>())
                    .add(ticket);
        }

        return defaulters.stream()
                .sorted(defaulterOrder())
                .map(summary -> {
                    List<Ticket> assignedTickets = ticketsByFieldPerson.getOrDefault(summary.getUserId(), List.of());
                    List<AgeingTicketDTO> ticketDtos = assignedTickets.stream()
                            .map(this::toAgeingTicket)
                            .toList();
                    long maxDaysOpen = ticketDtos.stream()
                            .mapToLong(AgeingTicketDTO::getDaysOpen)
                            .max()
                            .orElse(0);
                    return AgeingReportDTO.builder()
                            .userId(summary.getUserId())
                            .userName(summary.getUserName())
                            .email(summary.getEmail())
                            .openTicketCount(summary.getOpenTicketCount())
                            .maxDaysOpen(maxDaysOpen)
                            .tickets(ticketDtos)
                            .build();
                })
                .toList();
    }

    private AgeingTicketDTO toAgeingTicket(Ticket ticket) {
        return AgeingTicketDTO.builder()
                .ticketId(ticket.getId())
                .title(ticket.getIncidentType() == null ? null : ticket.getIncidentType().getName())
                .createdAt(TicketQueueRules.ticketStartTime(ticket))
                .daysOpen(daysOpen(TicketQueueRules.ticketStartTime(ticket)))
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .build();
    }

    private long daysOpen(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0;
        }
        LocalDate todayInReportZone = LocalDate.now(TicketQueueRules.REPORT_ZONE);
        return ChronoUnit.DAYS.between(createdAt.toLocalDate(), todayInReportZone);
    }

    private Comparator<AgeingAssigneeSummary> defaulterOrder() {
        return Comparator.comparingLong((AgeingAssigneeSummary summary) ->
                        Objects.requireNonNullElse(summary.getOpenTicketCount(), 0L))
                .reversed()
                .thenComparing(Comparator.comparingLong(
                        (AgeingAssigneeSummary summary) -> daysOpen(summary.getOldestCreatedAt())).reversed());
    }
}
