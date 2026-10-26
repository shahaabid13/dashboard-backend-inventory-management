package com.inventory.msp.services;

import com.inventory.msp.dto.CreateMaintenanceRequestRequest;
import com.inventory.msp.model.*;
import com.inventory.msp.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository requestRepo;
    private final DeviceRepository deviceRepo;
    private final DeviceHistoryRepository historyRepo;
    private final LocationRepository locationRepo;
    private final ApproachRoadRepository roadRepo;

    private static final Logger log = LoggerFactory.getLogger(MaintenanceRequestService.class);

    public MaintenanceRequest createRequest(CreateMaintenanceRequestRequest req) {

        Device device = deviceRepo.findBySerialNumber(req.getDeviceSerial())
                .orElseThrow(() -> new RuntimeException("Device not found for serial: " + req.getDeviceSerial()));

        if (requestRepo.existsByDeviceIdAndStatus(device.getId(), MaintenanceRequestStatus.PENDING)) {
            throw new RuntimeException("A pending maintenance request already exists for this device.");
        }

        if (req.getRequestType() == MaintenanceRequestType.REPLACE) {

            if (req.getNewSerial() == null || req.getNewSerial().isBlank()) {
                throw new RuntimeException("Replacement requires a newSerial.");
            }

            String newSerial = req.getNewSerial().trim();
            String oldSerial = req.getOldSerial() != null ? req.getOldSerial().trim() : "";

            if (newSerial.equalsIgnoreCase(oldSerial)) {
                throw new RuntimeException("New serial cannot be same as old serial.");
            }

            Device replacement = deviceRepo.findAll().stream()
                    .filter(d -> d.getSerialNumber().equalsIgnoreCase(newSerial))
                    .findFirst()
                    .orElse(null);

            if (replacement != null) {

                String status = replacement.getStatus() != null ? replacement.getStatus().trim() : "";

                if (status.equalsIgnoreCase(DeviceStatus.SPARE)) {
                    log.info("Replacement device {} is spare and ready for use.", newSerial);
                } else if (status.equalsIgnoreCase(DeviceStatus.INSTALLED)
                        || status.equalsIgnoreCase(DeviceStatus.RELOCATED)
                        || status.equalsIgnoreCase(DeviceStatus.UNDER_REPAIR)) {

                    throw new RuntimeException("Replacement device " + newSerial +
                            " is currently active in another location (" +
                            (replacement.getLocation() != null ? replacement.getLocation().getName() : "Unknown") +
                            "). Please select a SPARE or new device.");
                }

                if (requestRepo.existsByNewSerialAndStatus(newSerial, MaintenanceRequestStatus.PENDING)) {
                    throw new RuntimeException("This replacement device already has a pending request.");
                }

            } else {
                log.warn("Replacement device {} not found — will be treated as NEW device upon approval.", newSerial);
                req.setRemarks((req.getRemarks() != null ? req.getRemarks() + " | " : "") + "NEW DEVICE detected.");
            }
        }

        Long newLocationId = null;
        if (req.getNewLocationName() != null && !req.getNewLocationName().isEmpty()) {
            Location loc = locationRepo.findByName(req.getNewLocationName())
                    .orElseThrow(() -> new RuntimeException("Location not found: " + req.getNewLocationName()));
            newLocationId = loc.getId();
        }

        Long newApproachRoadId = null;
        if (req.getNewApproachRoadName() != null && !req.getNewApproachRoadName().isEmpty()) {

            if (newLocationId == null)
                throw new RuntimeException("Approach road requires newLocationName");

            Location loc = locationRepo.findById(newLocationId)
                    .orElseThrow(() -> new RuntimeException("Location missing unexpectedly"));

            ApproachRoad road = roadRepo.findByRoadNameAndLocationIgnoreCase(req.getNewApproachRoadName(), loc)
                    .orElseThrow(() -> new RuntimeException("Approach road not found: " + req.getNewApproachRoadName()));

            newApproachRoadId = road.getId();
        }

        String refId = "REQ-" + UUID.randomUUID().toString().substring(0, 8);

        MaintenanceRequest r = new MaintenanceRequest();
        r.setReferenceId(refId);
        r.setDeviceId(device.getId());
        r.setOldSerial(req.getOldSerial());
        r.setNewSerial(req.getNewSerial());
        r.setRequestType(req.getRequestType());
        r.setStatus(MaintenanceRequestStatus.PENDING);
        r.setCreatedBy(req.getCreatedBy());
        r.setRemarks(req.getRemarks());
        r.setCreatedAt(LocalDateTime.now());
        r.setNewLocationId(newLocationId);
        r.setNewApproachRoadId(newApproachRoadId);

        return requestRepo.save(r);
    }

    public MaintenanceRequest approveRequest(Long id, String admin, boolean approve, String remarks) {

        MaintenanceRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        req.setApprovedBy(admin);
        req.setRemarks(remarks);

        if (!approve) {
            req.setStatus(MaintenanceRequestStatus.REJECTED);
            return requestRepo.save(req);
        }

        req.setStatus(MaintenanceRequestStatus.APPROVED);
        req.setUpdatedAt(LocalDateTime.now());

        processRequest(req);

        return requestRepo.save(req);
    }

    private void processRequest(MaintenanceRequest req) {

        Device device = deviceRepo.findById(req.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        String oldSerial       = device.getSerialNumber();
        String oldLocationName = device.getLocation() != null ? device.getLocation().getName() : null;
        String newLocationName = null;

        if (req.getNewSerial() != null) {
            boolean exists = deviceRepo.existsBySerialNumber(req.getNewSerial());
            if (exists && !req.getNewSerial().equalsIgnoreCase(device.getSerialNumber())) {
                throw new RuntimeException("Serial number already exists for another device.");
            }
        }

        // ← FIXED: Boolean.TRUE.equals() instead of isPlaceholder()
        if (Boolean.TRUE.equals(device.getPlaceholder())) {
            if (req.getNewSerial() == null || req.getNewSerial().isBlank()) {
                throw new RuntimeException("Placeholder devices require a new serial number.");
            }
        }

        if (req.getRequestType() != MaintenanceRequestType.MOVE &&
                (req.getNewLocationId() != null || req.getNewApproachRoadId() != null)) {
            throw new RuntimeException("Device can only be moved using a MOVE request.");
        }

        switch (req.getRequestType()) {
            case SPARE:        device.setStatus(DeviceStatus.SPARE);        break;
            case FAULT:        device.setStatus(DeviceStatus.FAULT);        break;
            case REPAIR:       device.setStatus(DeviceStatus.UNDER_REPAIR); break;
            case MOVE:         device.setStatus(DeviceStatus.RELOCATED);    break;
            case REPLACE:      device.setStatus(DeviceStatus.REPLACED);     break;
            case SERIAL_UPDATE: /* keep original status */                  break;
        }

        if (req.getRequestType() == MaintenanceRequestType.SERIAL_UPDATE &&
                req.getNewSerial() != null) {
            device.setSerialNumber(req.getNewSerial());
            device.setPlaceholder(false);   // ← auto-boxed Boolean, fine
        }

        if (req.getRequestType() == MaintenanceRequestType.MOVE &&
                req.getNewLocationId() != null) {

            Location newLoc = locationRepo.findById(req.getNewLocationId())
                    .orElseThrow(() -> new RuntimeException("Location not found"));

            newLocationName = newLoc.getName();

            ApproachRoad newRoad = null;
            if (req.getNewApproachRoadId() != null) {
                newRoad = roadRepo.findById(req.getNewApproachRoadId()).orElse(null);
            }

            device.setLocation(newLoc);
            device.setApproachRoad(newRoad);
        }

        if (req.getRequestType() == MaintenanceRequestType.REPLACE &&
                req.getNewSerial() != null) {

            Device newDevice = deviceRepo.findBySerialNumber(req.getNewSerial())
                    .orElseThrow(() ->
                            new RuntimeException("Replacement device not found: " + req.getNewSerial()));

            device.setStatus(DeviceStatus.REPLACED);

            newDevice.setStatus(DeviceStatus.INSTALLED);
            newDevice.setPlaceholder(false);    // ← auto-boxed Boolean, fine
            newDevice.setLocation(device.getLocation());
            newDevice.setApproachRoad(device.getApproachRoad());

            deviceRepo.save(device);
            deviceRepo.save(newDevice);

            historyRepo.save(DeviceHistory.builder()
                    .deviceId(device.getId())
                    .action("Replace")
                    .oldSerial(oldSerial)
                    .newSerial(newDevice.getSerialNumber())
                    .oldLocation(oldLocationName)
                    .newLocation(newDevice.getLocation() != null ? newDevice.getLocation().getName() : null)
                    .replacedDeviceSerial(req.getNewSerial())
                    .referenceId(req.getReferenceId())
                    .createdAt(LocalDateTime.now())
                    .build());

            return;
        }

        device.setUpdatedAt(LocalDateTime.now());
        deviceRepo.save(device);

        historyRepo.save(DeviceHistory.builder()
                .deviceId(device.getId())
                .action("Maintenance Action: " + req.getRequestType())
                .oldSerial(oldSerial)
                .newSerial(device.getSerialNumber())
                .oldLocation(oldLocationName)
                .newLocation(newLocationName)
                .replacedDeviceSerial(req.getNewSerial())
                .referenceId(req.getReferenceId())
                .createdAt(LocalDateTime.now())
                .build());
    }

    public MaintenanceRequest getById(Long id) {
        return requestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }

    public List<MaintenanceRequest> getAll() {
        return requestRepo.findAll();
    }
}