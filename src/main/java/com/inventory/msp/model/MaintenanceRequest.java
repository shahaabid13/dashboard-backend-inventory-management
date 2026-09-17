package com.inventory.msp.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceId;          // the device being acted upon

    private String oldSerial;
    private String newSerial;

    private Long newLocationId;     // for MOVE or REPLACE
    private Long newApproachRoadId;

    private String createdBy;       // agency user
    private String approvedBy;      // admin user

    private String referenceId;     // for external sync
    private String remarks;

    @Enumerated(EnumType.STRING)
    private MaintenanceRequestType requestType;

    @Enumerated(EnumType.STRING)
    private MaintenanceRequestStatus status = MaintenanceRequestStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
