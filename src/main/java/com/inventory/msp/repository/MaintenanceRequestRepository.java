package com.inventory.msp.repository;

import com.inventory.msp.model.MaintenanceRequest;

import com.inventory.msp.model.MaintenanceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> existsByStatus(MaintenanceRequestStatus status);

    List<MaintenanceRequest> findByCreatedBy(String username);

    boolean existsByDeviceIdAndStatus(Long id, MaintenanceRequestStatus maintenanceRequestStatus);

    boolean existsByNewSerialAndStatus(String newSerial, MaintenanceRequestStatus maintenanceRequestStatus);
}
