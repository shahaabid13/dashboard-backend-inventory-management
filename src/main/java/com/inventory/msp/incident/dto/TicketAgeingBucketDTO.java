package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TicketAgeingBucketDTO {
    String bucket;
    Long count;
}
