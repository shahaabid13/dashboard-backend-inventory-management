package com.inventory.msp.incident.model;

import com.inventory.msp.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "ticket_history")
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private AppUser changedByUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by_user_id")
    private AppUser performedByUser;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 50)
    private TicketAction action;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(length = 500)
    private String notes;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "performed_at")
    private LocalDateTime performedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to_user_id")
    private AppUser assignedToUser;

    @Column(name = "assigned_to_role", length = 50)
    private String assignedToRole;

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
        if (this.performedAt == null) {
            this.performedAt = this.changedAt;
        }
        if (this.performedByUser == null) {
            this.performedByUser = this.changedByUser;
        }
        if (this.remarks == null && this.notes != null) {
            this.remarks = this.notes;
        }
        if (this.notes == null && this.remarks != null) {
            this.notes = this.remarks;
        }
    }
}

