package com.inventory.msp.repository;

import com.inventory.msp.model.ApproachRoad;
import com.inventory.msp.model.Device;
import com.inventory.msp.model.DeviceType;
import com.inventory.msp.model.Location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findBySerialNumber(String serial);

   boolean existsBySerialNumber(String serial);

    List<Device> findByLocation(Location location);

    List<Device> findByApproachRoad(ApproachRoad approachRoad);

    List<Device> findByDeviceType(DeviceType type);

    List<Device> findByPlaceholder(boolean placeholder); // missing serials

//    List<Device> findByStatus(String status);
//
//    List<Device> findByLocationAndDeviceType(Location location, DeviceType type);
//
//    List<Device> findByLocationAndPlaceholder(Location location, boolean placeholder);
//
//    List<Device> findByLocationAndDeviceTypeAndPlaceholder(
//            Location location, DeviceType type, boolean placeholder
//    );
    @Query("""
    SELECT d FROM Device d
    WHERE (:locationName IS NULL OR LOWER(TRIM(d.location.name)) = LOWER(TRIM(:locationName)))
    AND (:type IS NULL OR d.deviceType = :type)
    AND (:missing IS NULL OR d.placeholder = :missing)
    AND (:status IS NULL OR LOWER(d.status) = LOWER(:status))
    """)
    List<Device> advancedSearch(String locationName,
                                DeviceType type,
                                Boolean missing,
                                String status);




}

