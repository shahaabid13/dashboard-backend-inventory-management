package com.inventory.msp.incident.service;

import com.inventory.msp.incident.dto.*;
import com.inventory.msp.incident.model.*;
import com.inventory.msp.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IncidentMapper {

    public TicketResponse toTicketResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .incidentTypeId(ticket.getIncidentType().getId())
                .incidentTypeName(ticket.getIncidentType().getName())
                .locationId(ticket.getLocation().getId())
                .locationName(ticket.getLocation().getName())
                .approachRoadId(ticket.getApproachRoad() != null ? ticket.getApproachRoad().getId() : null)
                .approachRoadName(ticket.getApproachRoad() != null ? ticket.getApproachRoad().getRoadName() : null)
                .deviceTypeId(ticket.getDeviceType() != null ? ticket.getDeviceType().getId() : null)
                .deviceTypeName(ticket.getDeviceType() != null ? ticket.getDeviceType().getName() : null)
                .fieldPersonId(ticket.getFieldPerson().getId())
                .fieldPersonName(ticket.getFieldPerson().getName())
                .priority(ticket.getPriority())
                .description(normalizeOptionalText(ticket.getDescription()))
                .status(ticket.getStatus())
                .raisedByUserId(ticket.getRaisedByUser().getId())
                .raisedByUsername(ticket.getRaisedByUser().getUsername())
                .coordinatorId(ticket.getCoordinator() != null ? ticket.getCoordinator().getId() : null)
                .coordinatorUsername(ticket.getCoordinator() != null ? ticket.getCoordinator().getUsername() : null)
                .reviewerId(ticket.getReviewer() != null ? ticket.getReviewer().getId() : null)
                .reviewerUsername(ticket.getReviewer() != null ? ticket.getReviewer().getUsername() : null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .coordinatorAckedAt(ticket.getCoordinatorAckedAt())
                .assignedAt(ticket.getAssignedAt())
                .closedAt(ticket.getClosedAt())
                .reopenedAt(ticket.getReopenedAt())
                .build();
    }

    public TicketDetailResponse toTicketDetailResponse(Ticket ticket, List<TicketHistory> history) {
        return toTicketDetailResponse(ticket, history, List.of());
    }

    public TicketDetailResponse toTicketDetailResponse(Ticket ticket, List<TicketHistory> history, List<String> allowedActions) {
        List<TicketHistory> normalizedHistory = normalizeHistory(history);
        return TicketDetailResponse.builder()
                .id(ticket.getId())
                .incidentTypeId(ticket.getIncidentType().getId())
                .incidentTypeName(ticket.getIncidentType().getName())
                .locationId(ticket.getLocation().getId())
                .locationName(ticket.getLocation().getName())
                .approachRoadId(ticket.getApproachRoad() != null ? ticket.getApproachRoad().getId() : null)
                .approachRoadName(ticket.getApproachRoad() != null ? ticket.getApproachRoad().getRoadName() : null)
                .deviceTypeId(ticket.getDeviceType() != null ? ticket.getDeviceType().getId() : null)
                .deviceTypeName(ticket.getDeviceType() != null ? ticket.getDeviceType().getName() : null)
                .fieldPersonId(ticket.getFieldPerson().getId())
                .fieldPersonName(ticket.getFieldPerson().getName())
                .fieldPersonPhone(ticket.getFieldPerson().getPhone())
                .priority(ticket.getPriority())
                .description(normalizeOptionalText(ticket.getDescription()))
                .status(ticket.getStatus())
                .raisedByUserId(ticket.getRaisedByUser().getId())
                .raisedByUsername(ticket.getRaisedByUser().getUsername())
                .coordinatorId(ticket.getCoordinator() != null ? ticket.getCoordinator().getId() : null)
                .coordinatorUsername(ticket.getCoordinator() != null ? ticket.getCoordinator().getUsername() : null)
                .coordinatorAckNotes(ticket.getCoordinatorAckNotes())
                .reviewerId(ticket.getReviewer() != null ? ticket.getReviewer().getId() : null)
                .reviewerUsername(ticket.getReviewer() != null ? ticket.getReviewer().getUsername() : null)
                .reviewNotes(ticket.getReviewNotes())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .coordinatorAckedAt(ticket.getCoordinatorAckedAt())
                .assignedAt(ticket.getAssignedAt())
                .closedAt(ticket.getClosedAt())
                .reopenedAt(ticket.getReopenedAt())
                .allowedActions(allowedActions == null ? List.of() : allowedActions)
                .history(normalizedHistory.stream().map(this::toTicketHistoryResponse).collect(Collectors.toList()))
                .build();
    }

    public TicketHistoryResponse toTicketHistoryResponse(TicketHistory history) {
        String notes = history.getRemarks() != null ? history.getRemarks() : history.getNotes();
        return TicketHistoryResponse.builder()
                .id(history.getId())
                .ticketId(history.getTicket().getId())
                .changedByUserId(history.getChangedByUser() != null ? history.getChangedByUser().getId() : null)
                .changedByUsername(history.getChangedByUser() != null ? history.getChangedByUser().getUsername() : null)
                .action(history.getAction() != null ? history.getAction().name() : null)
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .notes(notes)
                .changedAt(history.getPerformedAt() != null ? history.getPerformedAt() : history.getChangedAt())
                .performedAt(history.getPerformedAt() != null ? history.getPerformedAt() : history.getChangedAt())
                .assignedToUserId(history.getAssignedToUser() != null ? history.getAssignedToUser().getId() : null)
                .assignedToUsername(history.getAssignedToUser() != null ? history.getAssignedToUser().getUsername() : null)
                .assignedToRole(history.getAssignedToRole())
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<TicketHistory> normalizeHistory(List<TicketHistory> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        Map<String, TicketHistory> merged = new LinkedHashMap<>();
        for (TicketHistory item : history) {
            String key = (item.getTicket() != null ? item.getTicket().getId() : "ticket") + ":"
                    + (item.getChangedByUser() != null ? item.getChangedByUser().getId() : "n/a") + ":"
                    + (item.getPerformedAt() != null ? item.getPerformedAt() : item.getChangedAt());
            TicketHistory existing = merged.get(key);
            if (existing == null) {
                merged.put(key, item);
                continue;
            }
            if (existing.getFromStatus() == null && item.getFromStatus() != null) {
                existing.setFromStatus(item.getFromStatus());
            }
            if (existing.getToStatus() == null && item.getToStatus() != null) {
                existing.setToStatus(item.getToStatus());
            }
            if (existing.getRemarks() == null && item.getRemarks() != null) {
                existing.setRemarks(item.getRemarks());
            }
            if (existing.getNotes() == null && item.getNotes() != null) {
                existing.setNotes(item.getNotes());
            }
            if (existing.getAction() == null && item.getAction() != null) {
                existing.setAction(item.getAction());
            }
        }
        return new ArrayList<>(merged.values());
    }

    public IncidentTypeResponse toIncidentTypeResponse(IncidentType incidentType) {
        return IncidentTypeResponse.builder()
                .id(incidentType.getId())
                .name(incidentType.getName())
                .active(incidentType.getActive())
                .createdAt(incidentType.getCreatedAt())
                .build();
    }

    public FieldPersonResponse toFieldPersonResponse(FieldPerson fieldPerson) {
        return FieldPersonResponse.builder()
                .id(fieldPerson.getId())
                .name(fieldPerson.getName())
                .role(fieldPerson.getRole())
                .phone(fieldPerson.getPhone())
                .active(fieldPerson.getActive())
                .createdAt(fieldPerson.getCreatedAt())
                .build();
    }

    public AppUserResponse toAppUserResponse(AppUser appUser) {
        return AppUserResponse.builder()
                .id(appUser.getId())
                .username(appUser.getUsername())
                .role(appUser.getRole() != null ? appUser.getRole().name() : null)
                .agencyName(appUser.getAgencyName())
                .fullName(appUser.getFullName())
                .email(appUser.getEmail())
                .phone(appUser.getPhone())
                .build();
    }
}

