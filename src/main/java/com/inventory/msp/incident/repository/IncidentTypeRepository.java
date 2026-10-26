package com.inventory.msp.incident.repository;

import com.inventory.msp.incident.model.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentTypeRepository extends JpaRepository<IncidentType, Long> {

    List<IncidentType> findByActive(Boolean active);
}

