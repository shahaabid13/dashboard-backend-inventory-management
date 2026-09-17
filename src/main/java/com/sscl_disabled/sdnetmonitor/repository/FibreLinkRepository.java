package com.sscl.sdnetmonitor.repository;

import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.FibreLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FibreLinkRepository extends JpaRepository<FibreLink, String> {

    List<FibreLink> findByFromJunctionIdOrToJunctionId(String fromId, String toId);

    List<FibreLink> findByCurrentStatus(DeviceStatus status);

    long countByCurrentStatus(DeviceStatus status);
}
