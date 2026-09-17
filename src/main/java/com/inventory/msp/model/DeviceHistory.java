package com.inventory.msp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceId;
    private String action;           // Installed, Fault, SerialUpdated, etc.

    private String oldSerial;
    private String newSerial;

    private String oldLocation;
    private String newLocation;

    private String replacedDeviceSerial;  // if replacement happened

    private String referenceId;          // request from agency

    private LocalDateTime createdAt = LocalDateTime.now();
}
