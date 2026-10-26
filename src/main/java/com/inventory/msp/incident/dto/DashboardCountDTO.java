package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardCountDTO {
    String userName;
    Long count;
}
