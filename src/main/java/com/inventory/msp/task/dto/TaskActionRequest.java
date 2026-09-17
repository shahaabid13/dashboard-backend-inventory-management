package com.inventory.msp.task.dto;

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
public class TaskActionRequest {

    @NotBlank(message = "Status is required")
    private String status;  // One of: RESOLVED, HOLD, REJECTED

    @NotBlank(message = "Summary is required (closing note)")
    private String summary;
}
