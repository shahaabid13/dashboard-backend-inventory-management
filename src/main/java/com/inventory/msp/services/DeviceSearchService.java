package com.inventory.msp.services;

import com.inventory.msp.model.ApproachRoad;
import com.inventory.msp.model.Device;
import com.inventory.msp.model.DeviceType;
import com.inventory.msp.model.Location;
import com.inventory.msp.repository.ApproachRoadRepository;
import com.inventory.msp.repository.DeviceRepository;
import com.inventory.msp.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceSearchService {

    private final DeviceRepository deviceRepo;
    private final LocationRepository locationRepo;
    private final ApproachRoadRepository roadRepo;

    public Device getBySerial(String serial) {
        return deviceRepo.findBySerialNumber(serial)
                .orElseThrow(() -> new RuntimeException("Device not found"));
    }

    public List<Device> getByLocation(Long locationId) {
        Location loc = locationRepo.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));
        return deviceRepo.findByLocation(loc);
    }

    public List<Device> getByApproachRoad(Long roadId) {
        ApproachRoad road = roadRepo.findById(roadId)
                .orElseThrow(() -> new RuntimeException("Approach road not found"));
        return deviceRepo.findByApproachRoad(road);
    }

    public List<Device> getByType(DeviceType type) {
        return deviceRepo.findByDeviceType(type);
    }




    public List<Device> getMissingSerials() {
        return deviceRepo.findByPlaceholder(true);
    }

    public List<Device> getInstalledDevices() {
        return deviceRepo.findByPlaceholder(false);
    }

    //Method To get by location and type of device ----------------------------------------------------------------------------------

//    public List<Device> getByLocationAndType(Long locationId, DeviceType type) {
//        Location loc = locationRepo.findById(locationId)
//                .orElseThrow(() -> new RuntimeException("Location not found"));
//        return deviceRepo.findByLocationAndDeviceType(loc, type);
//    }

    //Method to get by location and status------------------------------------------------------------------------------------------

//    public List<Device> getByLocationAndStatus(Long locationId, boolean placeholder) {
//        Location loc = locationRepo.findById(locationId)
//                .orElseThrow(() -> new RuntimeException("Location not found"));
//        return deviceRepo.findByLocationAndPlaceholder(loc, placeholder);
//    }
//
//    public List<Device> getFilteredDevices(Long locationId, DeviceType type, Boolean missing) {
//
//        Location loc = locationRepo.findById(locationId)
//                .orElseThrow(() -> new RuntimeException("Location not found"));
//
//        if (type != null && missing != null) {
//            return deviceRepo.findByLocationAndDeviceTypeAndPlaceholder(loc, type, missing);
//        }
//        if (type != null) {
//            return deviceRepo.findByLocationAndDeviceType(loc, type);
//        }
//        if (missing != null) {
//            return deviceRepo.findByLocationAndPlaceholder(loc, missing);
//        }
//
//        return deviceRepo.findByLocation(loc);
//    }

    public List<Device> advancedSearch(String locationName, DeviceType type, String status) {

        Boolean missing = null;     // used only for placeholder filter
        String normalizedStatus = null;

        if (status != null && !status.trim().isEmpty()) {
            normalizedStatus = status.trim().toLowerCase();

            // If user specifically searches for “missing”
            if (normalizedStatus.equals("missing")) {
                missing = true;                // placeholder = true
                normalizedStatus = null;       // no need to match status column
            }

            //  If user searches for installed
            else if (normalizedStatus.equals("installed")) {
                missing = false;               // placeholder = false
                // But also match status column
                normalizedStatus = "installed";
            }

            //  For ANY OTHER status → just match in DB
            else {
                missing = null;                // do not filter placeholder
                // pass this status to repo
            }
        }

        return deviceRepo.advancedSearch(
                locationName != null ? locationName.trim() : null,
                type,
                missing,              //  true / false / null
                normalizedStatus      // "installed" OR custom status OR null
        );
    }


}
