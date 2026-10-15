package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TaskAssigneeCountDTO {
    String userName;
    Long pending;
    Long overdue;
}
