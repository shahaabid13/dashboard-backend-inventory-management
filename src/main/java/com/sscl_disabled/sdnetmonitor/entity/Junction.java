package com.sscl.sdnetmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A physical ICCC field location: a fibre junction, a router/PoP, or the
 * Data Center itself. id is a stable human-readable slug (e.g. "hyderpora"),
 * not a surrogate key -- it doubles as the natural join key used by
 * fibre_links and is what the seed JSON files reference directly.
 */
@Entity
@Table(name = "junctions")
@Getter
@Setter
@NoArgsConstructor
public class Junction {

    @Id
    @Column(name = "id", length = 80)
    private String id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private JunctionType type;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "has_coordinates", nullable = false)
    private boolean hasCoordinates;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
