package com.inventory.msp.incident.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketReassignmentRequest {

    @NotNull(message = "Field person ID is required")
    private Long fieldPersonId;

    @NotNull(message = "Reassignment date is required")
    private LocalDate scheduledDate;

    private String remarks;
}
