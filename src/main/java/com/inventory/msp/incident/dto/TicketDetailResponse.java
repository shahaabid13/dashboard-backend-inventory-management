package com.inventory.msp.incident.dto;

import com.inventory.msp.incident.model.TicketPriority;
import com.inventory.msp.incident.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponse {

    private Long id;
    private Long incidentTypeId;
    private String incidentTypeName;
    private Long locationId;
    private String locationName;
    private Long approachRoadId;
    private String approachRoadName;
    private Long deviceTypeId;
    private String deviceTypeName;
    private Long fieldPersonId;
    private String fieldPersonName;
    private String fieldPersonPhone;
    private TicketPriority priority;
    private String description;
    private TicketStatus status;
    private Long raisedByUserId;
    private String raisedByUsername;
    private Long coordinatorId;
    private String coordinatorUsername;
    private String coordinatorAckNotes;
    private Long reviewerId;
    private String reviewerUsername;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private LocalDateTime coordinatorAckedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime closedAt;
    private LocalDateTime reopenedAt;

    private List<TicketHistoryResponse> history;
}

