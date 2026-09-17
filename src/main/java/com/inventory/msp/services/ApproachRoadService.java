package com.inventory.msp.services;

import com.inventory.msp.model.ApproachRoad;
import com.inventory.msp.model.Location;
import com.inventory.msp.repository.ApproachRoadRepository;
import com.inventory.msp.repository.LocationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApproachRoadService {

    private final ApproachRoadRepository roadRepo;
    private final LocationRepository locationRepo;

    //  Always load roads with location initialized
    public List<ApproachRoad> all() {
        return roadRepo.findAllWithLocation();
    }

    //  Fetch roads by location name
    public List<ApproachRoad> byLocationName(String locationName) {
        return roadRepo.findByLocationName(locationName.trim());
    }

    // Fetch roads by location id
    public List<ApproachRoad> byLocationId(Long locationId) {
        return roadRepo.findByLocationId(locationId);
    }

    public ApproachRoad create(String name, Long locationId) {
        Location loc = locationRepo.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        ApproachRoad road = new ApproachRoad();
        road.setRoadName(name);
        road.setLocation(loc);

        return roadRepo.save(road);
    }
}
