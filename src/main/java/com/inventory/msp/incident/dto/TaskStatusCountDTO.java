package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TaskStatusCountDTO {
    String status;
    Long count;
}
