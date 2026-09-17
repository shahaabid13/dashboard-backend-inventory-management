package com.inventory.msp.incident.model;

import com.inventory.msp.model.AppUser;
import com.inventory.msp.model.Location;
import com.inventory.msp.model.ApproachRoad;
import com.inventory.msp.model.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_type_id", nullable = false)
    private IncidentType incidentType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approach_road_id", nullable = true)
    private ApproachRoad approachRoad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_type_id")
    private com.inventory.msp.model.DeviceTypeEntity deviceType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "field_person_id", nullable = false)
    private FieldPerson fieldPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'MEDIUM'")
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'OPEN'")
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "raised_by_user_id", nullable = false)
    private AppUser raisedByUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coordinator_id", nullable = true)
    private AppUser coordinator;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewer_id", nullable = true)
    private AppUser reviewer;

    @Column(length = 500)
    private String coordinatorAckNotes;

    @Column(length = 500)
    private String reviewNotes;

    private LocalDateTime createdAt;
    private LocalDateTime coordinatorAckedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime closedAt;
    private LocalDateTime reopenedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketHistory> history = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

