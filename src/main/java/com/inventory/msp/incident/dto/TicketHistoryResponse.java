package com.inventory.msp.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHistoryResponse {

    private Long id;
    private Long ticketId;
    private Long changedByUserId;
    private String changedByUsername;
    private String fromStatus;
    private String toStatus;
    private String notes;
    private LocalDateTime changedAt;
}

