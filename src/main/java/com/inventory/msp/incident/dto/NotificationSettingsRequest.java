package com.inventory.msp.incident.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsRequest {

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phone;

    private Boolean emailEnabled;
    private Boolean smsEnabled;

    private Boolean notifyEmailTicketCreated;
    private Boolean notifyEmailTicketAcknowledged;
    private Boolean notifyEmailTicketAssigned;
    private Boolean notifyEmailTicketResolved;
    private Boolean notifyEmailTicketOnHold;
    private Boolean notifyEmailTicketReopened;
    private Boolean notifyEmailTicketRejected;

    private Boolean notifySmsTicketCreated;
    private Boolean notifySmsTicketAcknowledged;
    private Boolean notifySmsTicketAssigned;
    private Boolean notifySmsTicketResolved;
    private Boolean notifySmsTicketOnHold;
    private Boolean notifySmsTicketReopened;
    private Boolean notifySmsTicketRejected;
}
