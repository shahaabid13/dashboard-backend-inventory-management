package com.inventory.msp.repository;

import com.inventory.msp.model.DeviceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceTypeRepository extends JpaRepository<DeviceTypeEntity, Long> {
    List<DeviceTypeEntity> findByActive(Boolean active);
}

