package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MyPendingTicketDTO {
    Long ticketId;
    String title;
    String location;
    long hoursPending;
    boolean flagged;
}
