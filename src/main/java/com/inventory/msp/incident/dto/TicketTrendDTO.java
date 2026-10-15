package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class TicketTrendDTO {
    LocalDate date;
    long created;
    long closed;
}
