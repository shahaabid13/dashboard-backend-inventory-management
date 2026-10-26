package com.inventory.msp.services;

import com.inventory.msp.dto.DeviceRequest;
import com.inventory.msp.exception.AlreadyExistsException;
import com.inventory.msp.model.*;
import com.inventory.msp.repository.ApproachRoadRepository;
import com.inventory.msp.repository.DeviceHistoryRepository;
import com.inventory.msp.repository.DeviceRepository;
import com.inventory.msp.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository deviceRepository;
    private final DeviceHistoryRepository historyRepository;
    private final LocationRepository locationRepository;
    private final ApproachRoadRepository approachRoadRepository;

    public String createDevice(DeviceRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getSerialNumber() == null || request.getSerialNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("serialNumber is required");
        }
        if (request.getLocationName() == null || request.getLocationName().trim().isEmpty()) {
            throw new IllegalArgumentException("locationName is required");
        }
        if (request.getApproachRoadName() == null || request.getApproachRoadName().trim().isEmpty()) {
            throw new IllegalArgumentException("approachRoadName is required");
        }

        String serial       = request.getSerialNumber().trim();
        String locationName = request.getLocationName().trim();
        String roadName     = request.getApproachRoadName().trim();

        Boolean poles       = Boolean.TRUE.equals(request.getPoles());
        Boolean ecbPresent  = Boolean.TRUE.equals(request.getEcbPresent());
        Boolean placeholder = request.isPlaceholder();

        if (deviceRepository.findBySerialNumber(serial).isPresent()) {
            throw new AlreadyExistsException(
                    "Device already exists with this serial number: " + serial
            );
        }

        Location location = locationRepository.findByNameIgnoreCase(locationName)
                .orElseGet(() -> {
                    Location newLoc = new Location();
                    newLoc.setName(locationName);
                    newLoc.setJunctionBox(JunctionBoxType.NONE);
                    return locationRepository.save(newLoc);
                });

        ApproachRoad approachRoad = approachRoadRepository
                .findByRoadNameAndLocationIgnoreCase(roadName, location)
                .orElseGet(() -> {
                    ApproachRoad newRoad = new ApproachRoad();
                    newRoad.setRoadName(roadName);
                    newRoad.setLocation(location);
                    return approachRoadRepository.save(newRoad);
                });

        Device device = Device.builder()
                .serialNumber(serial)
                .deviceType(request.getDeviceType())
                .junctionBoxType(request.getJunctionBoxType())
                .poles(poles)
                .ecbPresent(ecbPresent)
                .placeholder(placeholder)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(request.getStatus())
                .location(location)
                .approachRoad(approachRoad)
                .notified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        deviceRepository.save(device);

        return "✅ Device created successfully at location: " + location.getName();
    }

    public List<Device> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        if (log.isDebugEnabled()) {
            devices.forEach(d -> log.debug("[DeviceService] Loaded device id={} serial='{}'", d.getId(), d.getSerialNumber()));
        }
        return devices;
    }

    public Device getDevice(Long id) {
        Device d = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        if (log.isDebugEnabled()) {
            log.debug("[DeviceService] getDevice id={} serial='{}'", d.getId(), d.getSerialNumber());
        }
        return d;
    }

    public Device getBySerial(String serial) {
        Device d = deviceRepository.findBySerialNumber(serial)
                .orElseThrow(() -> new RuntimeException("Serial not found"));
        if (log.isDebugEnabled()) {
            log.debug("[DeviceService] getBySerial lookup='{}' -> id={} serial='{}'", serial, d.getId(), d.getSerialNumber());
        }
        return d;
    }

    public Device saveDevice(Device device) {
        return deviceRepository.save(device);
    }

    public void updateNotified(Long deviceId, boolean notified) {
        Device device = getDevice(deviceId);
        device.setNotified(notified);
        device.setUpdatedAt(LocalDateTime.now());
        deviceRepository.save(device);
    }

    public Device updateSerialNumber(Long deviceId, String newSerial) {

        Device device = getDevice(deviceId);

        if (!Boolean.TRUE.equals(device.getPlaceholder())) {
            throw new RuntimeException("Device is not a placeholder, cannot update serial directly");
        }

        if (deviceRepository.existsBySerialNumber(newSerial)) {
            throw new RuntimeException("Serial already exists");
        }

        String oldSerial       = device.getSerialNumber();
        String oldLocationName = device.getLocation() != null
                ? device.getLocation().getName() : null;

        device.setSerialNumber(newSerial);
        device.setPlaceholder(false);
        device.setUpdatedAt(LocalDateTime.now());

        Device saved = deviceRepository.save(device);

        DeviceHistory history = DeviceHistory.builder()
                .deviceId(saved.getId())
                .action("Serial Update (placeholder replaced)")
                .oldSerial(oldSerial)
                .newSerial(newSerial)
                .oldLocation(oldLocationName)
                .newLocation(oldLocationName)
                .replacedDeviceSerial(null)
                .referenceId("MANUAL-UPDATE")
                .createdAt(LocalDateTime.now())
                .build();

        historyRepository.save(history);

        return saved;
    }

    public Device updateDevice(Long id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + id));

        // case-insensitive lookup, auto-create if missing (matches createDevice behavior)
        Location location = locationRepository.findByNameIgnoreCase(request.getLocationName())
                .orElseGet(() -> {
                    Location newLoc = new Location();
                    newLoc.setName(request.getLocationName().trim());
                    newLoc.setJunctionBox(JunctionBoxType.NONE);
                    return locationRepository.save(newLoc);
                });

        // filter by location too, auto-create if missing
        ApproachRoad road = null;
        if (request.getApproachRoadName() != null && !request.getApproachRoadName().isBlank()) {
            road = approachRoadRepository
                    .findByRoadNameAndLocationIgnoreCase(request.getApproachRoadName().trim(), location)
                    .orElseGet(() -> {
                        ApproachRoad newRoad = new ApproachRoad();
                        newRoad.setRoadName(request.getApproachRoadName().trim());
                        newRoad.setLocation(location);
                        return approachRoadRepository.save(newRoad);
                    });
        }

        device.setSerialNumber(request.getSerialNumber());
        device.setDeviceType(request.getDeviceType());
        device.setStatus(request.getStatus());
        device.setLatitude(request.getLatitude());
        device.setLongitude(request.getLongitude());
        device.setPoles(Boolean.TRUE.equals(request.getPoles()));
        device.setEcbPresent(Boolean.TRUE.equals(request.getEcbPresent()));
        device.setPlaceholder(request.isPlaceholder());
        // preserve existing notified value if not provided in request
        device.setNotified(request.getNotified() != null ? request.getNotified() : device.getNotified());
        device.setLocation(location);
        device.setApproachRoad(road);
        device.setUpdatedAt(LocalDateTime.now());

        return deviceRepository.save(device);
    }
}