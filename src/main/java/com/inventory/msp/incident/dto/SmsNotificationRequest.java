package com.inventory.msp.incident.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsNotificationRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format (must be E.164 format or start with +)")
    private String to;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Ticket ID is required")
    @Min(value = 1, message = "Ticket ID must be greater than 0")
    private Long ticketId;

    @NotBlank(message = "Event type is required")
    private String eventType;
}
