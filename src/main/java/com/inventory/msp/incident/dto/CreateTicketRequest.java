package com.inventory.msp.incident.dto;

import com.inventory.msp.incident.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {

    @NotNull(message = "Incident type ID is required")
    private Long incidentTypeId;

    @NotNull(message = "Location ID is required")
    private Long locationId;

    private Long approachRoadId;

    private Long deviceTypeId;

    @NotNull(message = "Field person ID is required")
    private Long fieldPersonId;

    @NotNull(message = "Priority is required")
    private TicketPriority priority;

    private String description;
}

