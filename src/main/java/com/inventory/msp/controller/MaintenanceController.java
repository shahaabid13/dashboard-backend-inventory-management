package com.inventory.msp.controller;

import com.inventory.msp.dto.*;
import com.inventory.msp.model.Device;
import com.inventory.msp.model.MaintenanceRequest;
import com.inventory.msp.services.DeviceService;
import com.inventory.msp.services.MaintenanceRequestService;
import com.inventory.msp.repository.LocationRepository;
import com.inventory.msp.repository.ApproachRoadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceRequestService maintenanceService;
    private final DeviceService deviceService;
    private final LocationRepository locationRepository;
    private final ApproachRoadRepository approachRoadRepository;

    //  AGENCY CREATES REQUEST
    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY')")
    public ResponseEntity<MaintenanceResponseDto> create(
            @RequestBody CreateMaintenanceRequestRequest req,
            Authentication auth) {

        //  Set createdBy from JWT token username
        req.setCreatedBy(auth.getName());

        //  Auto-fetch device using serial (agency never sends ID)
        if (req.getDeviceSerial() != null) {
            Device device = deviceService.getBySerial(req.getDeviceSerial());
            req.setOldSerial(device.getSerialNumber());  //  overwrite unsafe oldSerial from request
        }


        MaintenanceRequest created = maintenanceService.createRequest(req);
        return ResponseEntity.ok(toResponseDto(created));
    }


    //  GET SINGLE REQUEST
    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY')")

    public ResponseEntity<MaintenanceRequestDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(maintenanceService.getById(id)));
    }

    //  LIST ALL
    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY')")

    public ResponseEntity<List<MaintenanceRequestDto>> list() {
        List<MaintenanceRequestDto> list = maintenanceService.getAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    //  ADMIN APPROVAL
    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")

    public ResponseEntity<MaintenanceRequestDto> approve(@PathVariable Long id,
                                                         @RequestBody ApproveRequestDto dto,
                                                         Authentication auth) {

        //  Always take approvedBy from JWT user — not from request payload
        String approvedBy = auth.getName();

        MaintenanceRequest out =
                maintenanceService.approveRequest(id, approvedBy, dto.isApproved(), dto.getRemarks());

        return ResponseEntity.ok(toDto(out));
    }

    //  Convert Entity → DTO
    private MaintenanceRequestDto toDto(MaintenanceRequest r) {
        MaintenanceRequestDto d = new MaintenanceRequestDto();
        d.setId(r.getId());
        d.setDeviceId(r.getDeviceId()); //device id
        d.setOldSerial(r.getOldSerial());   //old serial of the device
        d.setNewSerial(r.getNewSerial());   //new serial of the device
        d.setRequestType(r.getRequestType());       //request type faulty, replace etc
        d.setStatus(r.getStatus());                 //current status of the request
        d.setNewLocationId(r.getNewLocationId());
        d.setNewApproachRoadId(r.getNewApproachRoadId());

        // Resolve location name from ID
        if (r.getNewLocationId() != null) {
            d.setNewLocationName(
                locationRepository.findById(r.getNewLocationId())
                    .map(loc -> loc.getName())
                    .orElse(null)
            );
        }

        // Resolve approach road name from ID
        if (r.getNewApproachRoadId() != null) {
            d.setNewApproachRoadName(
                approachRoadRepository.findById(r.getNewApproachRoadId())
                    .map(road -> road.getRoadName())
                    .orElse(null)
            );
        }

        d.setCreatedBy(r.getCreatedBy());
        d.setApprovedBy(r.getApprovedBy());
        d.setReferenceId(r.getReferenceId());
        d.setRemarks(r.getRemarks());

        // Ensure timestamps are included in DTO
        d.setCreatedAt(r.getCreatedAt());
        d.setUpdatedAt(r.getUpdatedAt());

        return d;
    }

    //Response DTO-----------------------------------------------------------------------------------

    private MaintenanceResponseDto toResponseDto(MaintenanceRequest r) {

        MaintenanceResponseDto d = new MaintenanceResponseDto();

        // Basic request details
        d.setId(r.getId());
        d.setDeviceId(r.getDeviceId());
        d.setOldSerial(r.getOldSerial());
        d.setNewSerial(r.getNewSerial());

        d.setRequestType(r.getRequestType());
        d.setStatus(r.getStatus());
        d.setCreatedBy(r.getCreatedBy());
        d.setApprovedBy(r.getApprovedBy());
        d.setReferenceId(r.getReferenceId());
        d.setRemarks(r.getRemarks());
        d.setCreatedAt(r.getCreatedAt());
        d.setUpdatedAt(r.getUpdatedAt());

        // ---------------------------
        // Resolve new location and road names from IDs (for MOVE requests)
        if (r.getNewLocationId() != null) {
            d.setNewLocationName(
                locationRepository.findById(r.getNewLocationId())
                    .map(loc -> loc.getName())
                    .orElse(null)
            );
        }

        if (r.getNewApproachRoadId() != null) {
            d.setNewApproachRoadName(
                approachRoadRepository.findById(r.getNewApproachRoadId())
                    .map(road -> road.getRoadName())
                    .orElse(null)
            );
        }

        // ---------------------------
        // EXTRA: Device + Location + Road
        // ---------------------------
        if (r.getDeviceId() != null) {

            Device device = deviceService.getDevice(r.getDeviceId());

            d.setDeviceType(device.getDeviceType());

            d.setCurrentSerial(device.getSerialNumber());

            if (device.getLocation() != null) {

                d.setLocationName(device.getLocation().getName());
            }

            if (device.getApproachRoad() != null) {

                d.setApproachRoadName(device.getApproachRoad().getRoadName());
            }
        }

        return d;
    }

}
