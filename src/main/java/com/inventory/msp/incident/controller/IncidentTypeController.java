package com.inventory.msp.incident.controller;

import com.inventory.msp.incident.dto.IncidentTypeRequest;
import com.inventory.msp.incident.dto.IncidentTypeResponse;
import com.inventory.msp.incident.model.IncidentType;
import com.inventory.msp.incident.service.IncidentMapper;
import com.inventory.msp.incident.service.IncidentTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidents/incident-types")
@RequiredArgsConstructor
public class IncidentTypeController {

    private final IncidentTypeService incidentTypeService;
    private final IncidentMapper incidentMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IncidentTypeResponse> createIncidentType(
            @Valid @RequestBody IncidentTypeRequest request) {
        IncidentType incidentType = incidentTypeService.createIncidentType(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentMapper.toIncidentTypeResponse(incidentType));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<IncidentTypeResponse> getIncidentType(@PathVariable Long id) {
        IncidentType incidentType = incidentTypeService.getIncidentType(id);
        return ResponseEntity.ok(incidentMapper.toIncidentTypeResponse(incidentType));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<List<IncidentTypeResponse>> getAllIncidentTypes() {
        List<IncidentType> incidentTypes = incidentTypeService.getAllIncidentTypes();
        return ResponseEntity.ok(incidentTypes.stream()
                .map(incidentMapper::toIncidentTypeResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<List<IncidentTypeResponse>> getActiveIncidentTypes() {
        List<IncidentType> incidentTypes = incidentTypeService.getActiveIncidentTypes();
        return ResponseEntity.ok(incidentTypes.stream()
                .map(incidentMapper::toIncidentTypeResponse)
                .collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IncidentTypeResponse> updateIncidentType(
            @PathVariable Long id,
            @Valid @RequestBody IncidentTypeRequest request) {
        IncidentType incidentType = incidentTypeService.updateIncidentType(id, request.getName(), request.getActive());
        return ResponseEntity.ok(incidentMapper.toIncidentTypeResponse(incidentType));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteIncidentType(@PathVariable Long id) {
        incidentTypeService.deleteIncidentType(id);
        return ResponseEntity.noContent().build();
    }
}
