package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class FlaggedTicketDTO {
    Long id;
    Long ticketId;
    String title;
    String assignee;
    String message;
    LocalDateTime createdAt;
    long hoursPending;
    int reminderCount;
    int remindersSent;
}
