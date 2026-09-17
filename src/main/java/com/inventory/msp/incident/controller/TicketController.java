package com.inventory.msp.incident.controller;

import com.inventory.msp.incident.dto.*;
import com.inventory.msp.incident.model.Ticket;
import com.inventory.msp.incident.model.TicketHistory;
import com.inventory.msp.incident.model.FieldPerson;
import com.inventory.msp.incident.service.IncidentMapper;
import com.inventory.msp.incident.service.TicketService;
import com.inventory.msp.incident.repository.FieldPersonRepository;
import com.inventory.msp.model.AppUser;
import com.inventory.msp.repository.UserRepository;
import com.inventory.msp.exception.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidents/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final IncidentMapper incidentMapper;
    private final UserRepository userRepository;
    private final FieldPersonRepository fieldPersonRepository;

    // ========== SUPPORT ENGINEER ENDPOINTS ==========

    @PostMapping
    @PreAuthorize("hasRole('SUPPORT_ENGINEER')")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Ticket ticket = ticketService.createTicket(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentMapper.toTicketResponse(ticket));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SUPPORT_ENGINEER')")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            Authentication authentication,
            Pageable pageable) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Page<Ticket> tickets = ticketService.getTicketsByRaisedByUser(user, pageable);
        return ResponseEntity.ok(tickets.map(incidentMapper::toTicketResponse));
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<Page<TicketResponse>> getMyHistory(
            Authentication authentication,
            @RequestParam(required = false) Boolean includeAll,
            Pageable pageable) {
        AppUser reviewer = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Page<Ticket> tickets;
        if (Boolean.TRUE.equals(includeAll)) {
            // Get all tickets ever assigned to this reviewer, regardless of status
            tickets = ticketService.getAllTicketsForReviewer(reviewer, pageable);
        } else {
            // Get only active tickets in review queue
            tickets = ticketService.getTicketsByReviewerQueue(reviewer, pageable);
        }
        return ResponseEntity.ok(tickets.map(incidentMapper::toTicketResponse));
    }

    // ========== FIELD PERSON ENDPOINTS ==========

    @GetMapping("/my-queue")
    @PreAuthorize("hasRole('FIELD_PERSON')")
    public ResponseEntity<?> getMyQueue(Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        FieldPerson fieldPerson = fieldPersonRepository.findByUserId(user.getId())
                .orElse(null);
        
        if (fieldPerson == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No associated field person profile found"));
        }
        
        List<Ticket> tickets = ticketService.getFieldPersonQueue(fieldPerson);
        return ResponseEntity.ok(tickets.stream()
                .map(incidentMapper::toTicketResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/my-ticket-history")
    @PreAuthorize("hasRole('FIELD_PERSON')")
    public ResponseEntity<?> getMyTicketHistory(
            Authentication authentication,
            Pageable pageable) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        FieldPerson fieldPerson = fieldPersonRepository.findByUserId(user.getId())
                .orElse(null);
        
        if (fieldPerson == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No associated field person profile found"));
        }
        
        Page<Ticket> tickets = ticketService.getAllTicketsForFieldPerson(fieldPerson, pageable);
        return ResponseEntity.ok(tickets.map(incidentMapper::toTicketResponse));
    }

    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('FIELD_PERSON')")
    public ResponseEntity<?> acknowledgeTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketActionRequest request,
            Authentication authentication) {
        AppUser fieldPerson = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Ticket ticket = ticketService.getTicket(id);
        
        FieldPerson fieldPersonProfile = fieldPersonRepository.findByUserId(fieldPerson.getId())
                .orElse(null);
        
        if (fieldPersonProfile == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No associated field person profile found"));
        }
        
        if (!ticket.getFieldPerson().getId().equals(fieldPersonProfile.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only acknowledge tickets assigned to you"));
        }
        
        Ticket updated = ticketService.acknowledgeTicket(id, request.getNotes(), fieldPerson);
        return ResponseEntity.ok(incidentMapper.toTicketResponse(updated));
    }

    @PutMapping("/{id}/assign-reviewer")
    @PreAuthorize("hasRole('FIELD_PERSON')")
    public ResponseEntity<?> assignReviewer(
            @PathVariable Long id,
            @Valid @RequestBody AssignReviewerRequest request,
            Authentication authentication) {
        AppUser fieldPerson = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Ticket ticket = ticketService.getTicket(id);
        
        if (ticket.getCoordinator() == null || !ticket.getCoordinator().getId().equals(fieldPerson.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only the field person who acknowledged this ticket can assign a reviewer"));
        }
        
        Ticket updated = ticketService.assignReviewer(id, request.getReviewerId(), fieldPerson);
        return ResponseEntity.ok(incidentMapper.toTicketResponse(updated));
    }

    @GetMapping("/reviewers")
    @PreAuthorize("hasAnyRole('FIELD_PERSON', 'SUPPORT_ENGINEER')")
    public ResponseEntity<List<AppUserResponse>> getReviewers() {
        List<AppUser> reviewers = userRepository.findByRole(com.inventory.msp.model.UserRole.REVIEWER);
        return ResponseEntity.ok(reviewers.stream()
                .map(incidentMapper::toAppUserResponse)
                .collect(Collectors.toList()));
    }

    // ========== REVIEWER ENDPOINTS ==========

    @GetMapping("/review-queue")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<Page<TicketResponse>> getReviewQueue(
            Authentication authentication,
            Pageable pageable) {
        AppUser reviewer = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Page<Ticket> tickets = ticketService.getTicketsByReviewerQueue(reviewer, pageable);
        return ResponseEntity.ok(tickets.map(incidentMapper::toTicketResponse));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketActionRequest request,
            Authentication authentication) {
        AppUser reviewer = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Ticket ticket = ticketService.resolveTicket(id, request.getNotes(), reviewer);
        return ResponseEntity.ok(incidentMapper.toTicketResponse(ticket));
    }

    @PutMapping("/{id}/pending")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<TicketResponse> holdTicketPending(
            @PathVariable Long id,
            @Valid @RequestBody TicketActionRequest request,
            Authentication authentication) {
        AppUser reviewer = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Ticket ticket = ticketService.holdTicketPending(id, request.getNotes(), reviewer);
        return ResponseEntity.ok(incidentMapper.toTicketResponse(ticket));
    }

    @PutMapping("/{id}/reopen")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<TicketResponse> reopenTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketActionRequest request,
            Authentication authentication) {
        AppUser reviewer = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Ticket ticket = ticketService.reopenTicket(id, request.getNotes(), reviewer);
        return ResponseEntity.ok(incidentMapper.toTicketResponse(ticket));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<TicketResponse> rejectTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketActionRequest request,
            Authentication authentication) {
        AppUser reviewer = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Ticket ticket = ticketService.rejectTicket(id, request.getNotes(), reviewer);
        return ResponseEntity.ok(incidentMapper.toTicketResponse(ticket));
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TicketResponse>> getAllTickets(
            @RequestParam(required = false) Long raisedByUserId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long incidentTypeId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Pageable pageable) {
        LocalDateTime from = null, to = null;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        if (fromDate != null) {
            from = LocalDateTime.parse(fromDate, formatter);
        }
        if (toDate != null) {
            to = LocalDateTime.parse(toDate, formatter);
        }

        Page<Ticket> tickets = ticketService.getTicketsWithFilters(
                raisedByUserId,
                status != null ? com.inventory.msp.incident.model.TicketStatus.valueOf(status) : null,
                incidentTypeId,
                locationId,
                from,
                to,
                pageable
        );
        return ResponseEntity.ok(tickets.map(incidentMapper::toTicketResponse));
    }

    // ========== COMMON VIEW ENDPOINTS ==========

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ENGINEER', 'FIELD_PERSON', 'REVIEWER')")
    public ResponseEntity<TicketDetailResponse> getTicketDetail(
            @PathVariable Long id,
            Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Ticket ticket = ticketService.getTicket(id);

        // Authorization: Admin can view all, others can only view if related
        boolean canView = user.getRole().name().equals("ADMIN") ||
                user.getId().equals(ticket.getRaisedByUser().getId()) ||
                (ticket.getCoordinator() != null && user.getId().equals(ticket.getCoordinator().getId())) ||
                (ticket.getReviewer() != null && user.getId().equals(ticket.getReviewer().getId())) ||
                (ticket.getFieldPerson() != null && ticket.getFieldPerson().getUser() != null
                        && user.getId().equals(ticket.getFieldPerson().getUser().getId()));

        if (!canView) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<TicketHistory> history = ticketService.getTicketHistory(id);
        return ResponseEntity.ok(incidentMapper.toTicketDetailResponse(ticket, history));
    }
}

