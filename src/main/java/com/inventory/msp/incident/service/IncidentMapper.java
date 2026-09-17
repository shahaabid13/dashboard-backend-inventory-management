package com.inventory.msp.incident.service;

import com.inventory.msp.incident.dto.*;
import com.inventory.msp.incident.model.*;
import com.inventory.msp.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .raisedByUserId(ticket.getRaisedByUser().getId())
                .raisedByUsername(ticket.getRaisedByUser().getUsername())
                .coordinatorId(ticket.getCoordinator() != null ? ticket.getCoordinator().getId() : null)
                .coordinatorUsername(ticket.getCoordinator() != null ? ticket.getCoordinator().getUsername() : null)
                .reviewerId(ticket.getReviewer() != null ? ticket.getReviewer().getId() : null)
                .reviewerUsername(ticket.getReviewer() != null ? ticket.getReviewer().getUsername() : null)
                .createdAt(ticket.getCreatedAt())
                .coordinatorAckedAt(ticket.getCoordinatorAckedAt())
                .assignedAt(ticket.getAssignedAt())
                .closedAt(ticket.getClosedAt())
                .reopenedAt(ticket.getReopenedAt())
                .build();
    }

    public TicketDetailResponse toTicketDetailResponse(Ticket ticket, List<TicketHistory> history) {
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
                .description(ticket.getDescription())
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
                .coordinatorAckedAt(ticket.getCoordinatorAckedAt())
                .assignedAt(ticket.getAssignedAt())
                .closedAt(ticket.getClosedAt())
                .reopenedAt(ticket.getReopenedAt())
                .history(history.stream().map(this::toTicketHistoryResponse).collect(Collectors.toList()))
                .build();
    }

    public TicketHistoryResponse toTicketHistoryResponse(TicketHistory history) {
        return TicketHistoryResponse.builder()
                .id(history.getId())
                .ticketId(history.getTicket().getId())
                .changedByUserId(history.getChangedByUser().getId())
                .changedByUsername(history.getChangedByUser().getUsername())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .notes(history.getNotes())
                .changedAt(history.getChangedAt())
                .build();
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

