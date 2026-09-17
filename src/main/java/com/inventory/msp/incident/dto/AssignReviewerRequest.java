package com.inventory.msp.incident.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignReviewerRequest {

    @NotNull(message = "Reviewer ID is required")
    private Long reviewerId;
}

