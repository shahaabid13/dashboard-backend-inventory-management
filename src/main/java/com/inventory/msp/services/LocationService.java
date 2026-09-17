package com.inventory.msp.services;

import com.inventory.msp.model.JunctionBoxType;
import com.inventory.msp.model.Location;
import com.inventory.msp.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;


    public Location getLocationByName(String location){
        return locationRepository.findByName(location)
                .orElseThrow(() -> new RuntimeException("Location not found: " + location));
    }

    public List<Location> getAll() {
        return locationRepository.findAll();
    }

    public Location getById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found"));
    }

    public Location create(String name, JunctionBoxType boxType) {

        if (locationRepository.findByName(name).isPresent())
            throw new RuntimeException("Location already exists");

        Location loc = new Location();
        loc.setName(name);
        loc.setJunctionBox(boxType);

        return locationRepository.save(loc);
    }

    public Location updateJunctionBox(Long id, JunctionBoxType boxType) {
        Location loc = getById(id);
        loc.setJunctionBox(boxType);
        return locationRepository.save(loc);
    }
}
