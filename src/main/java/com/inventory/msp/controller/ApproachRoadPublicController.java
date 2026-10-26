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
@RequestMapping("/api/approach-roads")
@RequiredArgsConstructor
public class ApproachRoadPublicController {

    private final ApproachRoadService roadService;

    // GET /api/approach-roads?locationId={id}
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<List<ApproachRoadDto>> all(@RequestParam(required = false) Long locationId) {
        List<ApproachRoad> roads = (locationId == null) ? roadService.all() : roadService.byLocationId(locationId);

        List<ApproachRoadDto> dto = roads.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    private ApproachRoadDto toDto(ApproachRoad road) {
        ApproachRoadDto dto = new ApproachRoadDto();
        dto.setId(road.getId());
        dto.setName(road.getRoadName());
        dto.setLocationId(road.getLocation() != null ? road.getLocation().getId() : null);
        dto.setLocationName(road.getLocation() != null ? road.getLocation().getName() : null);
        return dto;
    }
}

