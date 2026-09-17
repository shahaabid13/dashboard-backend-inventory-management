package com.inventory.msp.incident.service;

import com.inventory.msp.incident.model.IncidentType;
import com.inventory.msp.incident.repository.IncidentTypeRepository;
import com.inventory.msp.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentTypeService {

    private final IncidentTypeRepository incidentTypeRepository;

    public IncidentType createIncidentType(String name) {
        IncidentType type = IncidentType.builder()
                .name(name)
                .active(true)
                .build();
        return incidentTypeRepository.save(type);
    }

    public IncidentType getIncidentType(Long id) {
        return incidentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident type not found with id: " + id));
    }

    public List<IncidentType> getAllIncidentTypes() {
        return incidentTypeRepository.findAll();
    }

    public List<IncidentType> getActiveIncidentTypes() {
        return incidentTypeRepository.findByActive(true);
    }

    public IncidentType updateIncidentType(Long id, String name, Boolean active) {
        IncidentType type = getIncidentType(id);
        if (name != null) {
            type.setName(name);
        }
        if (active != null) {
            type.setActive(active);
        }
        return incidentTypeRepository.save(type);
    }

    public void deleteIncidentType(Long id) {
        IncidentType type = getIncidentType(id);
        // Perform soft-delete to avoid FK constraint issues: mark as inactive
        type.setActive(false);
        incidentTypeRepository.save(type);
    }
}
