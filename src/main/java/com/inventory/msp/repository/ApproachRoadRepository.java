package com.inventory.msp.repository;

import com.inventory.msp.model.ApproachRoad;
import com.inventory.msp.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ApproachRoadRepository extends JpaRepository<ApproachRoad, Long> {

    // Fetch all approach roads with location for listing
    @Query("""
        SELECT ar FROM ApproachRoad ar
        JOIN FETCH ar.location
    """)
    List<ApproachRoad> findAllWithLocation();

    // Fetch roads by location name (for UI dropdown etc.)
    @Query("""
        SELECT ar FROM ApproachRoad ar
        JOIN FETCH ar.location loc
        WHERE LOWER(TRIM(loc.name)) = LOWER(TRIM(:locationName))
    """)
    List<ApproachRoad> findByLocationName(String locationName);

    // Correct lookup: roadName + location must match (case-insensitive)
    @Query("""
        SELECT ar FROM ApproachRoad ar
        WHERE LOWER(TRIM(ar.roadName)) = LOWER(TRIM(:roadName))
        AND ar.location = :location
    """)
    Optional<ApproachRoad> findByRoadNameAndLocationIgnoreCase(String roadName, Location location);

    Optional<ApproachRoad> findByRoadNameIgnoreCaseAndLocationId(String roadName, Long locationId);

    // Fetch roads by location id with location initialized
    @Query("""
        SELECT ar FROM ApproachRoad ar
        JOIN FETCH ar.location loc
        WHERE loc.id = :locationId
    """)
    List<ApproachRoad> findByLocationId(Long locationId);
}
