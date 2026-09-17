package com.sscl.sdnetmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Append-only log of every device status transition. Uptime/downtime is
 * always computed from consecutive rows here (see UptimeCalculationService),
 * never stored as a running total -- that keeps the figure re-derivable and
 * correct no matter what time window is later asked for.
 */
@Entity
@Table(name = "device_status_events")
@Getter
@Setter
@NoArgsConstructor
public class DeviceStatusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 15)
    private DeviceStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 15)
    private DeviceStatus newStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private EventSource source = EventSource.PING;

    @Column(name = "note", length = 255)
    private String note;
}
