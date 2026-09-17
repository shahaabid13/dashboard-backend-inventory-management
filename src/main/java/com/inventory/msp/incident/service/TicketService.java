package com.inventory.msp.incident.service;

import com.inventory.msp.incident.dto.CreateTicketRequest;
import com.inventory.msp.incident.dto.DailyTicketCountResponse;
import com.inventory.msp.incident.exception.InvalidTicketStateTransitionException;
import com.inventory.msp.incident.model.*;
import com.inventory.msp.incident.repository.FieldPersonRepository;
import com.inventory.msp.incident.repository.TicketHistoryRepository;
import com.inventory.msp.incident.repository.TicketRepository;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.Location;
import com.inventory.msp.model.ApproachRoad;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.repository.LocationRepository;
import com.inventory.msp.repository.ApproachRoadRepository;
import com.inventory.msp.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final IncidentTypeService incidentTypeService;
    private final FieldPersonService fieldPersonService;
    private final FieldPersonRepository fieldPersonRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final ApproachRoadRepository approachRoadRepository;
    private final com.inventory.msp.repository.DeviceTypeRepository deviceTypeRepository;
    private final com.inventory.msp.incident.service.NotificationService notificationService;

    // ========== TICKET CREATION ==========

    @Transactional
    public Ticket createTicket(CreateTicketRequest request, Long raisedByUserId) {
        // Verify all references exist
        IncidentType incidentType = incidentTypeService.getIncidentType(request.getIncidentTypeId());
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new NotFoundException("Location not found"));
        FieldPerson fieldPerson = fieldPersonService.getFieldPerson(request.getFieldPersonId());
        AppUser raisedByUser = userRepository.findById(raisedByUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ApproachRoad approachRoad = null;
        if (request.getApproachRoadId() != null) {
            approachRoad = approachRoadRepository.findById(request.getApproachRoadId())
                    .orElseThrow(() -> new NotFoundException("Approach road not found"));
        }

        com.inventory.msp.model.DeviceTypeEntity deviceTypeEntity = null;
        if (request.getDeviceTypeId() != null) {
            deviceTypeEntity = deviceTypeRepository.findById(request.getDeviceTypeId())
                    .orElseThrow(() -> new NotFoundException("Device type not found"));
        }

        Ticket ticket = Ticket.builder()
                .incidentType(incidentType)
                .location(location)
                .approachRoad(approachRoad)
                .deviceType(deviceTypeEntity)
                .fieldPerson(fieldPerson)
                .priority(request.getPriority())
                .description(request.getDescription())
                .status(TicketStatus.OPEN)
                .raisedByUser(raisedByUser)
                .build();

        Ticket saved = ticketRepository.save(ticket);

        // Record initial creation in history
        recordStatusChange(saved, null, TicketStatus.OPEN.name(), "Ticket created", raisedByUser);

                // Notify (do not block creation)
                try {
                    notificationService.notifyForTicketEvent(saved.getId(), "TICKET_CREATED");
                } catch (Exception e) {
                    // already handled inside notificationService, but guard anyway
                }

                return saved;
    }

    // ========== TICKET RETRIEVAL ==========

    public Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found with id: " + id));
    }

    public Page<Ticket> getTicketsByRaisedByUser(AppUser user, Pageable pageable) {
        return ticketRepository.findByRaisedByUser(user, pageable);
    }

    public Page<Ticket> getTicketsByReviewerQueue(AppUser reviewer, Pageable pageable) {
        List<TicketStatus> reviewerStatuses = Arrays.asList(
                TicketStatus.ASSIGNED_TO_REVIEWER,
                TicketStatus.PENDING,
                TicketStatus.RESOLVED,
                TicketStatus.REOPENED,
                TicketStatus.REJECTED
        );
        return ticketRepository.findByReviewerAndStatusIn(reviewer, reviewerStatuses, pageable);
    }

    public Page<Ticket> getAllTicketsForReviewer(AppUser reviewer, Pageable pageable) {
        return ticketRepository.findByReviewerOrderByCreatedAtDesc(reviewer, pageable);
    }

    public Page<Ticket> getAllTicketsForCoordinator(AppUser coordinator, Pageable pageable) {
        return ticketRepository.findByCoordinatorOrderByCreatedAtDesc(coordinator, pageable);
    }

    public Page<Ticket> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable);
    }

    public Page<Ticket> getTicketsWithFilters(Long raisedByUserId, TicketStatus status, Long incidentTypeId,
                                               Long locationId, LocalDateTime fromDate, LocalDateTime toDate,
                                               Pageable pageable) {
        return ticketRepository.findWithFilters(raisedByUserId, status, incidentTypeId, locationId, fromDate, toDate, pageable);
    }

    public List<Ticket> getCoordinatorQueue() {
        // Include COORDINATOR_REVIEW so acknowledged-but-unassigned tickets appear in the coordinator queue
        return ticketRepository.findByStatusInOrderByCreatedAtAsc(
                Arrays.asList(TicketStatus.OPEN, TicketStatus.REOPENED, TicketStatus.COORDINATOR_REVIEW)
        );
    }

    public List<Ticket> getFieldPersonQueue(FieldPerson fieldPerson) {
        return ticketRepository.findByFieldPersonAndStatusInOrderByCreatedAtAsc(
                fieldPerson,
                Arrays.asList(TicketStatus.OPEN, TicketStatus.REOPENED)
        );
    }

    public Page<Ticket> getAllTicketsForFieldPerson(FieldPerson fieldPerson, Pageable pageable) {
        return ticketRepository.findByFieldPersonOrderByCreatedAtDesc(fieldPerson, pageable);
    }

    // ========== STATE MACHINE TRANSITIONS ==========

    @Transactional
    public Ticket acknowledgeTicket(Long ticketId, String notes, AppUser fieldPerson) {
        Ticket ticket = getTicket(ticketId);

        // Validate transition: OPEN or REOPENED -> COORDINATOR_REVIEW
        if (!Arrays.asList(TicketStatus.OPEN, TicketStatus.REOPENED).contains(ticket.getStatus())) {
            throw new InvalidTicketStateTransitionException(
                    "Cannot acknowledge ticket in status: " + ticket.getStatus()
            );
        }

        ticket.setStatus(TicketStatus.COORDINATOR_REVIEW);
        ticket.setCoordinator(fieldPerson);
        ticket.setCoordinatorAckNotes(notes);
        ticket.setCoordinatorAckedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, ticket.getStatus().name(), TicketStatus.COORDINATOR_REVIEW.name(), notes, fieldPerson);

        try {
            notificationService.notifyForTicketEvent(saved.getId(), "TICKET_ACKNOWLEDGED");
        } catch (Exception e) { }

        return saved;
    }
    @Transactional
    public Ticket assignReviewer(Long ticketId, Long reviewerId, AppUser coordinator) {
        Ticket ticket = getTicket(ticketId);

        // Validate transition: COORDINATOR_REVIEW -> ASSIGNED_TO_REVIEWER
        if (ticket.getStatus() != TicketStatus.COORDINATOR_REVIEW) {
            throw new InvalidTicketStateTransitionException(
                    "Cannot assign reviewer. Ticket must be in COORDINATOR_REVIEW status. Current status: " + ticket.getStatus()
            );
        }

        AppUser reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Reviewer not found with id: " + reviewerId));

        ticket.setStatus(TicketStatus.ASSIGNED_TO_REVIEWER);
        ticket.setReviewer(reviewer);
        ticket.setAssignedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, TicketStatus.COORDINATOR_REVIEW.name(), TicketStatus.ASSIGNED_TO_REVIEWER.name(),
                "Assigned to reviewer: " + reviewer.getUsername(), coordinator);

                try {
                    notificationService.notifyForTicketEvent(saved.getId(), "TICKET_ASSIGNED");
                } catch (Exception e) { }

                return saved;
    }

    @Transactional
    public Ticket resolveTicket(Long ticketId, String notes, AppUser reviewer) {
        Ticket ticket = getTicket(ticketId);

        // Validate transition: ASSIGNED_TO_REVIEWER or PENDING -> RESOLVED
        if (!Arrays.asList(TicketStatus.ASSIGNED_TO_REVIEWER, TicketStatus.PENDING).contains(ticket.getStatus())) {
            throw new InvalidTicketStateTransitionException(
                    "Cannot resolve ticket in status: " + ticket.getStatus()
            );
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setReviewNotes(notes);
        ticket.setClosedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, ticket.getStatus().name(), TicketStatus.RESOLVED.name(), notes, reviewer);

                try {
                    notificationService.notifyForTicketEvent(saved.getId(), "TICKET_RESOLVED");
                } catch (Exception e) { }

                return saved;
    }

    @Transactional
    public Ticket holdTicketPending(Long ticketId, String notes, AppUser reviewer) {
        Ticket ticket = getTicket(ticketId);

        // Validate transition: ASSIGNED_TO_REVIEWER -> PENDING
        if (ticket.getStatus() != TicketStatus.ASSIGNED_TO_REVIEWER) {
            throw new InvalidTicketStateTransitionException(
                    "Cannot hold ticket in status: " + ticket.getStatus() + ". Must be ASSIGNED_TO_REVIEWER."
            );
        }

        ticket.setStatus(TicketStatus.PENDING);
        ticket.setReviewNotes(notes);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, TicketStatus.ASSIGNED_TO_REVIEWER.name(), TicketStatus.PENDING.name(), notes, reviewer);

                try {
                    notificationService.notifyForTicketEvent(saved.getId(), "TICKET_ON_HOLD");
                } catch (Exception e) { }

                return saved;
    }

    @Transactional
    public Ticket reopenTicket(Long ticketId, String notes, AppUser reviewer) {
        Ticket ticket = getTicket(ticketId);

        // Validate transition: RESOLVED -> REOPENED
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidTicketStateTransitionException(
                    "Cannot reopen ticket in status: " + ticket.getStatus() + ". Must be RESOLVED."
            );
        }

        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setReopenedAt(LocalDateTime.now());
        ticket.setReviewNotes(notes);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, TicketStatus.RESOLVED.name(), TicketStatus.REOPENED.name(), notes, reviewer);

                try {
                    notificationService.notifyForTicketEvent(saved.getId(), "TICKET_REOPENED");
                } catch (Exception e) { }

                return saved;
    }

    @Transactional
    public Ticket rejectTicket(Long ticketId, String notes, AppUser reviewer) {
        Ticket ticket = getTicket(ticketId);

        // Validate transition: ASSIGNED_TO_REVIEWER -> REJECTED
        if (ticket.getStatus() != TicketStatus.ASSIGNED_TO_REVIEWER) {
            throw new InvalidTicketStateTransitionException(
                    "Cannot reject ticket in status: " + ticket.getStatus() + ". Must be ASSIGNED_TO_REVIEWER."
            );
        }

        ticket.setStatus(TicketStatus.REJECTED);
        ticket.setReviewNotes(notes);
        ticket.setClosedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, TicketStatus.ASSIGNED_TO_REVIEWER.name(), TicketStatus.REJECTED.name(), notes, reviewer);

                try {
                    notificationService.notifyForTicketEvent(saved.getId(), "TICKET_REJECTED");
                } catch (Exception e) { }

                return saved;
    }

    @Transactional
    public Ticket resumeReviewFromPending(Long ticketId, AppUser reviewer) {
        Ticket ticket = getTicket(ticketId);

        // Validate transition: PENDING -> ASSIGNED_TO_REVIEWER
        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new InvalidTicketStateTransitionException(
                    "Cannot resume review. Ticket must be in PENDING status. Current status: " + ticket.getStatus()
            );
        }

        ticket.setStatus(TicketStatus.ASSIGNED_TO_REVIEWER);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, TicketStatus.PENDING.name(), TicketStatus.ASSIGNED_TO_REVIEWER.name(),
                "Resumed review", reviewer);

        return saved;
    }

    // ========== HISTORY RECORDING ==========

    private void recordStatusChange(Ticket ticket, String fromStatus, String toStatus, String notes, AppUser changedByUser) {
        // Ensure toStatus is never null — fallback to ticket.status or OPEN
        if (toStatus == null) {
            if (ticket != null && ticket.getStatus() != null) {
                toStatus = ticket.getStatus().name();
            } else {
                toStatus = TicketStatus.OPEN.name();
            }
        }

        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .changedByUser(changedByUser)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .notes(notes)
                .build();
        ticketHistoryRepository.save(history);
    }

    public List<TicketHistory> getTicketHistory(Long ticketId) {
        Ticket ticket = getTicket(ticketId);
        return ticketHistoryRepository.findByTicketOrderByChangedAtDesc(ticket);
    }

    // ========== STATISTICS ==========

    public Map<String, Long> getTicketStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("TOTAL", (long) ticketRepository.findAll().size());
        stats.put("OPEN", ticketRepository.countByStatus(TicketStatus.OPEN));
        stats.put("COORDINATOR_REVIEW", ticketRepository.countByStatus(TicketStatus.COORDINATOR_REVIEW));
        stats.put("ASSIGNED_TO_REVIEWER", ticketRepository.countByStatus(TicketStatus.ASSIGNED_TO_REVIEWER));
        stats.put("PENDING", ticketRepository.countByStatus(TicketStatus.PENDING));
        stats.put("RESOLVED", ticketRepository.countByStatus(TicketStatus.RESOLVED));
        stats.put("REOPENED", ticketRepository.countByStatus(TicketStatus.REOPENED));
        stats.put("REJECTED", ticketRepository.countByStatus(TicketStatus.REJECTED));
        return stats;
    }

    public Map<String, Long> getPriorityBreakdown() {
        Map<String, Long> breakdown = new HashMap<>();
        List<Ticket> allTickets = ticketRepository.findAll();

        breakdown.put("LOW", allTickets.stream().filter(t -> t.getPriority() == TicketPriority.LOW).count());
        breakdown.put("MEDIUM", allTickets.stream().filter(t -> t.getPriority() == TicketPriority.MEDIUM).count());
        breakdown.put("HIGH", allTickets.stream().filter(t -> t.getPriority() == TicketPriority.HIGH).count());

        return breakdown;
    }

    public List<DailyTicketCountResponse> getDailyTicketCounts() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);  // 30 days inclusive (today - 29 = 30 days total)
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = today.atTime(23, 59, 59);
        
        // Query all tickets created in the last 30 days
        List<Ticket> tickets = ticketRepository.findByCreatedAtBetween(startDateTime, endDateTime);
        
        // Build a map of date (YYYY-MM-DD) -> count, using TreeMap to keep sorted
        Map<String, Long> dateCountMap = new TreeMap<>();
        
        // Initialize all dates in the range with 0
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(today)) {
            dateCountMap.put(currentDate.toString(), 0L);
            currentDate = currentDate.plusDays(1);
        }
        
        // Count tickets by creation date
        for (Ticket ticket : tickets) {
            if (ticket.getCreatedAt() != null) {
                LocalDate ticketDate = ticket.getCreatedAt().toLocalDate();
                String dateKey = ticketDate.toString();
                dateCountMap.put(dateKey, dateCountMap.getOrDefault(dateKey, 0L) + 1);
            }
        }
        
        // Convert map to list of DailyTicketCountResponse, maintaining order
        List<DailyTicketCountResponse> result = new java.util.ArrayList<>();
        for (Map.Entry<String, Long> entry : dateCountMap.entrySet()) {
            result.add(DailyTicketCountResponse.builder()
                    .date(entry.getKey())
                    .count(entry.getValue())
                    .build());
        }
        
        return result;
    }
}
