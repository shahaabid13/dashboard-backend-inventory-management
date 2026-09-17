package com.sscl.sdnetmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Any monitorable IP endpoint at a junction: the fibre-facing switch, a
 * CCTV/ANPR camera, an ECB, a PA speaker, a UPS, or Data Center
 * server/storage/application hosts. is_network_switch marks the one device
 * per junction (two, at the Data Center) whose reachability stands in for
 * that junction's fibre uplink until real per-interface SNMP data exists.
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "junction_id", nullable = false)
    private Junction junction;

    @Column(name = "device_label", nullable = false, length = 200)
    private String deviceLabel;

    @Column(name = "ip_address", nullable = false, length = 45, unique = true)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private DeviceCategory category;

    @Column(name = "is_network_switch", nullable = false)
    private boolean networkSwitch;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 15)
    private DeviceStatus currentStatus = DeviceStatus.UNKNOWN;

    @Column(name = "last_status_change")
    private Instant lastStatusChange;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    /** Last time a ping actually succeeded, regardless of whether that
     *  caused a status transition -- distinct from lastStatusChange (only
     *  moves on a transition) and lastCheckedAt (moves on every check,
     *  success or failure). This is the field that answers "how long has
     *  this device actually been down" / "when did we last confirm it was
     *  reachable" for SLA purposes without replaying the event log. */
    @Column(name = "last_up_at")
    private Instant lastUpAt;

    @Column(name = "snmp_enabled", nullable = false)
    private boolean snmpEnabled;

    @Column(name = "snmp_community", length = 100)
    private String snmpCommunity;

    @Column(name = "lldp_enabled")
    private Boolean lldpEnabled;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
