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
import com.inventory.msp.model.UserRole;
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
import java.util.Locale;
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
    private final TicketWorkflowService ticketWorkflowService;

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
                .description(normalizeDescription(request.getDescription()))
                .status(TicketStatus.OPEN)
                .raisedByUser(raisedByUser)
                .build();

        Ticket saved = ticketRepository.save(ticket);

        recordStatusChange(saved, null, TicketStatus.OPEN, raisedByUser, TicketAction.TICKET_CREATED,
                "Ticket created", fieldPerson != null ? fieldPerson.getUser() : null, UserRole.FIELD_PERSON);

                // Notify (do not block creation)
                try {
                    notificationService.notifyForTicketEvent(saved.getId(), "TICKET_CREATED");
                } catch (Exception e) {
                    // already handled inside notificationService, but guard anyway
                }

                return saved;
    }

    // ========== TICKET RETRIEVAL ==========

    @Transactional(readOnly = true)
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

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRemarks(String remarks) {
        if (remarks == null) {
            return null;
        }
        String normalized = remarks.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private TicketAction parseAction(String rawAction) {
        if (rawAction == null || rawAction.isBlank()) {
            throw new InvalidTicketStateTransitionException("Action is required");
        }
        String normalized = rawAction.trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("REVALIDATION".equals(upper)) {
            return TicketAction.REVALIDATION_REQUESTED;
        }
        if ("REOPEN".equals(upper)) {
            return TicketAction.REOPENED;
        }
        if ("SEND_FOR_REVIEW".equals(upper)) {
            return TicketAction.SENT_FOR_REVIEW;
        }
        if ("REASSIGN".equals(upper)) {
            return TicketAction.REASSIGNED;
        }
        try {
            return TicketAction.valueOf(upper);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTicketStateTransitionException("Unsupported action: " + rawAction);
        }
    }

    @Transactional
    public Ticket handleFieldPersonAction(Long ticketId, String rawAction, String remarks, AppUser actor) {
        Ticket ticket = getTicket(ticketId);
        TicketAction action = parseAction(rawAction);
        validateActionRemarks(action, remarks);
        ticketWorkflowService.validateTransition(ticket, actor, action);

        AppUser assignedTo = ticketWorkflowService.resolveAssignee(ticket, action);
        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = ticketWorkflowService.resolveStatus(action);

        ticket.setStatus(nextStatus);
        if (TicketAction.RESOLVED.equals(action)) {
            ticket.setReviewer(assignedTo);
            ticket.setClosedAt(LocalDateTime.now());
            ticket.setReopenedAt(null);
        } else if (TicketAction.REVALIDATION_REQUESTED.equals(action)) {
            ticket.setReviewer(null);
            ticket.setClosedAt(null);
        }
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus, nextStatus, actor, action, remarks, assignedTo, UserRole.FIELD_PERSON);

        String event = switch (action) {
            case RESOLVED -> "TICKET_RESOLVED";
            case REVALIDATION_REQUESTED -> "TICKET_REVALIDATION_REQUESTED";
            default -> "TICKET_ACKNOWLEDGED";
        };
        try {
            notificationService.notifyForTicketEvent(saved.getId(), event);
        } catch (Exception ignored) {
        }
        return saved;
    }

    @Transactional
    public Ticket handleSupportEngineerAction(Long ticketId, String rawAction, String remarks, AppUser actor) {
        Ticket ticket = getTicket(ticketId);
        TicketAction action = parseAction(rawAction);
        validateActionRemarks(action, remarks);
        ticketWorkflowService.validateTransition(ticket, actor, action);

        AppUser assignedTo = ticketWorkflowService.resolveAssignee(ticket, action);
        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = ticketWorkflowService.resolveStatus(action);

        ticket.setStatus(nextStatus);
        if (TicketAction.REOPENED.equals(action)) {
            ticket.setFieldPerson(ticket.getFieldPerson());
            ticket.setReviewer(null);
            ticket.setReopenedAt(LocalDateTime.now());
        }
        if (TicketAction.SENT_FOR_REVIEW.equals(action)) {
            ticket.setReviewer(assignedTo);
            ticket.setClosedAt(null);
            ticket.setReopenedAt(null);
        }
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus, nextStatus, actor, action, remarks, assignedTo,
                TicketAction.REOPENED.equals(action) ? UserRole.FIELD_PERSON : UserRole.REVIEWER);

        String event = switch (action) {
            case REOPENED -> "TICKET_REOPENED";
            case SENT_FOR_REVIEW -> "TICKET_SENT_FOR_REVIEW";
            default -> "TICKET_ACKNOWLEDGED";
        };
        try {
            notificationService.notifyForTicketEvent(saved.getId(), event);
        } catch (Exception ignored) {
        }
        return saved;
    }

    @Transactional
    public Ticket reassignTicketToFieldPerson(Long ticketId, Long fieldPersonId, LocalDate scheduledDate, String remarks, AppUser actor) {
        if (actor == null) {
            throw new InvalidTicketStateTransitionException("Actor is required");
        }
        if (!UserRole.SUPPORT_ENGINEER.equals(actor.getRole())) {
            throw new InvalidTicketStateTransitionException("Only a support engineer can reassign tickets");
        }

        Ticket ticket = getTicket(ticketId);
        if (ticket.getRaisedByUser() == null || !ticket.getRaisedByUser().getId().equals(actor.getId())) {
            throw new InvalidTicketStateTransitionException("Only the ticket creator can reassign this ticket");
        }
        if (ticket.getStatus() != TicketStatus.OPEN
                && ticket.getStatus() != TicketStatus.REOPENED
                && ticket.getStatus() != TicketStatus.REVALIDATION) {
            throw new InvalidTicketStateTransitionException(
                    "Ticket can only be reassigned while OPEN, REOPENED, or REVALIDATION. Current status: " + ticket.getStatus());
        }
        if (fieldPersonId == null) {
            throw new InvalidTicketStateTransitionException("Field person ID is required");
        }
        if (scheduledDate == null || scheduledDate.isBefore(LocalDate.now())) {
            throw new InvalidTicketStateTransitionException("Reassignment date cannot be in the past");
        }

        FieldPerson assignedFieldPerson = fieldPersonRepository.findById(fieldPersonId)
                .orElseThrow(() -> new NotFoundException("Field person not found with id: " + fieldPersonId));
        if (!Boolean.TRUE.equals(assignedFieldPerson.getActive())) {
            throw new InvalidTicketStateTransitionException("Selected field person is not active");
        }
        if (assignedFieldPerson.getUser() == null) {
            throw new InvalidTicketStateTransitionException("Selected field person is not linked to a valid user account");
        }

        TicketStatus previousStatus = ticket.getStatus();
        ticket.setFieldPerson(assignedFieldPerson);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setAssignedAt(scheduledDate.atStartOfDay());
        ticket.setReviewer(null);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus, TicketStatus.OPEN, actor, TicketAction.REASSIGNED,
                normalizeRemarks(remarks), assignedFieldPerson.getUser(), UserRole.FIELD_PERSON);

        try {
            notificationService.notifyForTicketEvent(saved.getId(), "TICKET_ASSIGNED");
        } catch (Exception ignored) {
        }
        return saved;
    }

    private void validateActionRemarks(TicketAction action, String remarks) {
        String normalized = normalizeRemarks(remarks);
        boolean required = action == TicketAction.RESOLVED || action == TicketAction.REVALIDATION_REQUESTED || action == TicketAction.REOPENED;
        if (required && (normalized == null || normalized.length() < 5)) {
            throw new InvalidTicketStateTransitionException("Remarks are required and must be at least 5 characters.");
        }
        if (action == TicketAction.SENT_FOR_REVIEW && normalized != null && normalized.length() < 5) {
            throw new InvalidTicketStateTransitionException("Remarks must be at least 5 characters when provided.");
        }
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

        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = TicketStatus.COORDINATOR_REVIEW;
        ticket.setStatus(nextStatus);
        ticket.setCoordinator(fieldPerson);
        ticket.setCoordinatorAckNotes(notes);
        ticket.setCoordinatorAckedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus.name(), nextStatus.name(), notes, fieldPerson);

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

        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = TicketStatus.ASSIGNED_TO_REVIEWER;
        ticket.setStatus(nextStatus);
        ticket.setReviewer(reviewer);
        ticket.setAssignedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus.name(), nextStatus.name(),
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

        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = TicketStatus.RESOLVED;
        ticket.setStatus(nextStatus);
        ticket.setReviewNotes(notes);
        ticket.setClosedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus.name(), nextStatus.name(), notes, reviewer);

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

        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = TicketStatus.PENDING;
        ticket.setStatus(nextStatus);
        ticket.setReviewNotes(notes);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus.name(), nextStatus.name(), notes, reviewer);

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

        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = TicketStatus.REOPENED;
        ticket.setStatus(nextStatus);
        ticket.setReopenedAt(LocalDateTime.now());
        ticket.setReviewNotes(notes);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus.name(), nextStatus.name(), notes, reviewer);

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

        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = TicketStatus.REJECTED;
        ticket.setStatus(nextStatus);
        ticket.setReviewNotes(notes);
        ticket.setClosedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus.name(), nextStatus.name(), notes, reviewer);

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

        TicketStatus previousStatus = ticket.getStatus();
        TicketStatus nextStatus = TicketStatus.ASSIGNED_TO_REVIEWER;
        ticket.setStatus(nextStatus);

        Ticket saved = ticketRepository.save(ticket);
        recordStatusChange(saved, previousStatus.name(), nextStatus.name(),
                "Resumed review", reviewer);

        return saved;
    }

    // ========== HISTORY RECORDING ==========

    private void recordStatusChange(Ticket ticket, String fromStatus, String toStatus, String notes, AppUser changedByUser) {
        TicketStatus from = fromStatus == null ? null : TicketStatus.valueOf(fromStatus);
        TicketStatus to = toStatus == null ? TicketStatus.OPEN : TicketStatus.valueOf(toStatus);
        recordStatusChange(ticket, from, to, changedByUser, null, notes, null, null);
    }

    private void recordStatusChange(Ticket ticket, TicketStatus fromStatus, TicketStatus toStatus,
                                   AppUser changedByUser, TicketAction action, String remarks,
                                   AppUser assignedToUser, UserRole assignedToRole) {
        String normalizedRemarks = normalizeRemarks(remarks);
        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .changedByUser(changedByUser)
                .performedByUser(changedByUser)
                .fromStatus(fromStatus != null ? fromStatus.name() : null)
                .toStatus(toStatus != null ? toStatus.name() : TicketStatus.OPEN.name())
                .action(action)
                .remarks(normalizedRemarks)
                .notes(normalizedRemarks)
                .assignedToUser(assignedToUser)
                .assignedToRole(assignedToRole != null ? assignedToRole.name() : null)
                .changedAt(LocalDateTime.now())
                .performedAt(LocalDateTime.now())
                .build();
        ticketHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<TicketHistory> getTicketHistory(Long ticketId) {
        Ticket ticket = getTicket(ticketId);
        return ticketHistoryRepository.findByTicketOrderByPerformedAtDescIdDesc(ticket);
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
        stats.put("REVALIDATION", ticketRepository.countByStatus(TicketStatus.REVALIDATION));
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
