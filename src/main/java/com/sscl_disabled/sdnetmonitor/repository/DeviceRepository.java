package com.sscl.sdnetmonitor.repository;

import com.sscl.sdnetmonitor.entity.Device;
import com.sscl.sdnetmonitor.entity.DeviceCategory;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByJunctionId(String junctionId);

    List<Device> findByJunctionIdAndNetworkSwitchTrue(String junctionId);

    Optional<Device> findFirstByJunctionIdAndNetworkSwitchTrue(String junctionId);

    List<Device> findByCategory(DeviceCategory category);

    List<Device> findByCurrentStatus(DeviceStatus status);

    long countByCurrentStatus(DeviceStatus status);
}
