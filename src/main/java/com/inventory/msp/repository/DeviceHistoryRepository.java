package com.inventory.msp.repository;

import com.inventory.msp.model.DeviceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceHistoryRepository extends JpaRepository<DeviceHistory, Long> {

    List<DeviceHistory> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

}
