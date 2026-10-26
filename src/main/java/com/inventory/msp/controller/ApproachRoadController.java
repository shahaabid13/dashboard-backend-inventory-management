package com.inventory.msp.controller;

import com.inventory.msp.dto.ApproachRoadDto;
import com.inventory.msp.model.ApproachRoad;
import com.inventory.msp.services.ApproachRoadService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roads")
@RequiredArgsConstructor
public class ApproachRoadController {

    private final ApproachRoadService roadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<List<ApproachRoadDto>> all() {
        return ResponseEntity.ok(
                roadService.all().stream().map(this::toDto).collect(Collectors.toList())
        );
    }

    @GetMapping("/location/{locationName}")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<List<ApproachRoadDto>> byLocation(@PathVariable String locationName) {
        return ResponseEntity.ok(
                roadService.byLocationName(locationName)
                        .stream()
                        .map(this::toDto)
                        .collect(Collectors.toList())
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApproachRoadDto> create(
            @RequestParam String name,
            @RequestParam Long locationId) {

        return ResponseEntity.ok(
                toDto(roadService.create(name, locationId))
        );
    }

    private ApproachRoadDto toDto(ApproachRoad road) {
        ApproachRoadDto dto = new ApproachRoadDto();
        dto.setId(road.getId());
        dto.setName(road.getRoadName());
        dto.setLocationId(road.getLocation().getId());
        dto.setLocationName(road.getLocation().getName());
        return dto;
    }
}
