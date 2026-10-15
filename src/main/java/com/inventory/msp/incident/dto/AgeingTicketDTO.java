package com.inventory.msp.incident.dto;

import com.inventory.msp.incident.model.TicketPriority;
import com.inventory.msp.incident.model.TicketStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AgeingTicketDTO {
    Long ticketId;
    String title;
    LocalDateTime createdAt;
    long daysOpen;
    TicketPriority priority;
    TicketStatus status;
}
