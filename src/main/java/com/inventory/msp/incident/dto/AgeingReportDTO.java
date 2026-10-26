package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AgeingReportDTO {
    Long userId;
    String userName;
    String email;
    long openTicketCount;
    long maxDaysOpen;
    List<AgeingTicketDTO> tickets;
}
