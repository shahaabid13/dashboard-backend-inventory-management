package com.sscl.sdnetmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One of the 109 physical fibre/duct segments traced from the SDNET KMZ
 * survey. from/to junctions may be null on a handful of segments whose
 * endpoint couldn't be resolved even approximately -- check `confirmed`
 * and `diagramConfirmed` before trusting either end for anything
 * operational (see SDNET_topology_match_audit.csv for the full reasoning
 * behind every segment's confidence rating).
 */
@Entity
@Table(name = "fibre_links")
@Getter
@Setter
@NoArgsConstructor
public class FibreLink {

    @Id
    @Column(name = "id", length = 191)
    private String id;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_junction_id")
    private Junction fromJunction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_junction_id")
    private Junction toJunction;

    @Column(name = "from_confident", nullable = false)
    private boolean fromConfident;

    @Column(name = "to_confident", nullable = false)
    private boolean toConfident;

    @Column(name = "length_meters")
    private Double lengthMeters;

    @Column(name = "confirmed", nullable = false)
    private boolean confirmed;

    @Column(name = "diagram_confirmed", nullable = false)
    private boolean diagramConfirmed;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 15)
    private DeviceStatus currentStatus = DeviceStatus.UNKNOWN;

    @Column(name = "last_status_change")
    private Instant lastStatusChange;

    /** Same purpose as Device.lastUpAt -- last time this link was confirmed
     *  up, independent of status-change events, for SLA reporting. */
    @Column(name = "last_up_at")
    private Instant lastUpAt;

    /** Raw JSON array of [lat, lon] pairs -- the traced polyline for the map.
     *  Kept as a plain string and (de)serialized in the service layer rather
     *  than mapped through a Hibernate JSON user-type, to avoid pulling in an
     *  extra dependency that can't be verified against Maven Central here. */
    @Column(name = "path_geojson", columnDefinition = "JSON")
    private String pathGeojson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
