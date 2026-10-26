package com.inventory.msp.controller;

import com.inventory.msp.dto.LocationDto;
import com.inventory.msp.model.JunctionBoxType;
import com.inventory.msp.model.Location;
import com.inventory.msp.services.LocationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    //  Everyone can view locations (support engineers and other incident roles need this)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<List<LocationDto>> getAll() {

        List<LocationDto> list = locationService.getAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    //  ADMIN only: Add new location
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationDto> create(
            @RequestParam String name,
            @RequestParam(defaultValue = "NONE") JunctionBoxType junctionBoxType) {

        return ResponseEntity.ok(
                toDto(locationService.create(name, junctionBoxType))
        );
    }

    //  ADMIN: Update junction box
    @PutMapping("/{id}/junction-box")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationDto> updateJunctionBox(
            @PathVariable Long id,
            @RequestParam JunctionBoxType type) {

        return ResponseEntity.ok(
                toDto(locationService.updateJunctionBox(id, type))
        );
    }

    //  Get location by name (secure)
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<LocationDto> getLocationByNames(@RequestParam String location){
        return ResponseEntity.ok(toDto(locationService.getLocationByName(location)));
    }

    private LocationDto toDto(Location loc) {
        LocationDto dto = new LocationDto();
        dto.setId(loc.getId());
        dto.setName(loc.getName());
        dto.setJunctionBox(loc.getJunctionBox() != null ? loc.getJunctionBox().name() : null);
        return dto;
    }
}
