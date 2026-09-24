package com.inventory.msp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serial_number", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonAlias({"serial","deviceSerial","serial_no","device_serial","serialNumber"})
    private String serialNumber;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean notified = true;   // ← wrapper, @Builder.Default now respected by JPA

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    private JunctionBoxType junctionBoxType;

    private Boolean poles;        // ← wrapper
    private Boolean ecbPresent;   // ← wrapper
    private Boolean placeholder;  // ← wrapper

    private String latitude;
    private String longitude;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approach_road_id")
    private ApproachRoad approachRoad;
}