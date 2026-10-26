package com.inventory.msp.incident.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "notification_settings")
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String email;
    private String phone;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean emailEnabled = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean smsEnabled = true;

    // Per-event email notifications
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifyEmailTicketCreated = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifyEmailTicketAcknowledged = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifyEmailTicketAssigned = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifyEmailTicketResolved = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifyEmailTicketOnHold = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifyEmailTicketReopened = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifyEmailTicketRejected = true;

    // Per-event SMS notifications
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifySmsTicketCreated = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifySmsTicketAcknowledged = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifySmsTicketAssigned = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifySmsTicketResolved = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifySmsTicketOnHold = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifySmsTicketReopened = true;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notifySmsTicketRejected = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean isNotificationEnabledForEvent(String eventType, String channel) {
        if ("EMAIL".equalsIgnoreCase(channel)) {
            if (!this.emailEnabled) return false;
            return switch (eventType) {
                case "TICKET_CREATED" -> this.notifyEmailTicketCreated;
                case "TICKET_ACKNOWLEDGED" -> this.notifyEmailTicketAcknowledged;
                case "TICKET_ASSIGNED" -> this.notifyEmailTicketAssigned;
                case "TICKET_RESOLVED" -> this.notifyEmailTicketResolved;
                case "TICKET_ON_HOLD" -> this.notifyEmailTicketOnHold;
                case "TICKET_REOPENED" -> this.notifyEmailTicketReopened;
                case "TICKET_REJECTED" -> this.notifyEmailTicketRejected;
                default -> false;
            };
        } else if ("SMS".equalsIgnoreCase(channel)) {
            if (!this.smsEnabled) return false;
            return switch (eventType) {
                case "TICKET_CREATED" -> this.notifySmsTicketCreated;
                case "TICKET_ACKNOWLEDGED" -> this.notifySmsTicketAcknowledged;
                case "TICKET_ASSIGNED" -> this.notifySmsTicketAssigned;
                case "TICKET_RESOLVED" -> this.notifySmsTicketResolved;
                case "TICKET_ON_HOLD" -> this.notifySmsTicketOnHold;
                case "TICKET_REOPENED" -> this.notifySmsTicketReopened;
                case "TICKET_REJECTED" -> this.notifySmsTicketRejected;
                default -> false;
            };
        }
        return false;
    }
}
