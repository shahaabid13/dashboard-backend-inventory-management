package com.sscl.sdnetmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Same purpose as DeviceStatusEvent, for fibre links. Today every row's
 *  source is DERIVED_PING (the link's status follows its two endpoint
 *  switches); once per-interface SNMP is wired in, new rows are written
 *  with source=SNMP instead and nothing about this table needs to change. */
@Entity
@Table(name = "fibre_link_status_events")
@Getter
@Setter
@NoArgsConstructor
public class FibreLinkStatusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fibre_link_id", nullable = false)
    private FibreLink fibreLink;

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
    private EventSource source = EventSource.DERIVED_PING;

    @Column(name = "note", length = 255)
    private String note;
}
