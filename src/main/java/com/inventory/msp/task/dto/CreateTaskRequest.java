package com.inventory.msp.task.dto;

import jakarta.validation.constraints.Min;
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
public class CreateTaskRequest {

    @NotNull(message = "Assigned to user ID is required")
    @Min(value = 1, message = "Assigned to user ID must be greater than 0")
    private Long assignedToUserId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
}
