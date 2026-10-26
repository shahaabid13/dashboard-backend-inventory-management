package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TopDefaulterDTO {
    Long userId;
    String userName;
    String email;
    long openTicketCount;
    long oldestTicketDays;
}
