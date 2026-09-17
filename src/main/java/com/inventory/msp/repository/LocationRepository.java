package com.inventory.msp.repository;

import com.inventory.msp.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    // ✅ Exact match (case-insensitive), used in service getLocationByName()
    @Query("""
        SELECT l FROM Location l 
        WHERE LOWER(TRIM(l.name)) = LOWER(TRIM(:name))
    """)
    Optional<Location> findByName(String name);

    Optional<Location> findByNameIgnoreCase(String name);

}
