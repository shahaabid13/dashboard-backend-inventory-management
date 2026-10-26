package com.inventory.msp.incident.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentTypeRequest {

    @NotBlank(message = "Incident type name is required")
    private String name;

    private Boolean active = true;
}

