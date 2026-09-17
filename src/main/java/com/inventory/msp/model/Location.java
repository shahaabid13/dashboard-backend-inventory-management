package com.inventory.msp.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Junction box belongs to the location
    @Enumerated(EnumType.STRING)
    private JunctionBoxType junctionBox = JunctionBoxType.NONE;

    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY)

    private List<ApproachRoad> approachRoads = new ArrayList<>();
}
