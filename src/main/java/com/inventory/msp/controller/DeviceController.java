package com.inventory.msp.controller;

import com.inventory.msp.dto.DeviceDto;
import com.inventory.msp.dto.DeviceRequest;
import com.inventory.msp.model.Device;
import com.inventory.msp.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    // Admin-only endpoint
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> createDevice(@RequestBody DeviceRequest request) {
        try {
            String response = deviceService.createDevice(request);
            return ResponseEntity.ok(response);
        } catch (com.inventory.msp.exception.AlreadyExistsException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create device: " + e.getMessage());
        }
    }

    // Get all devices
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<List<DeviceDto>> allDevices() {
        List<DeviceDto> result = deviceService.getAllDevices()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/debug-auth")
    public ResponseEntity<?> debugAuth(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "name", authentication.getName(),
                "authorities", authentication.getAuthorities().toString(),
                "principal", authentication.getPrincipal().toString()
        ));
    }

    // Get device by ID
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY','VIEWER')")
    public ResponseEntity<DeviceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(deviceService.getDevice(id)));
    }

    // Get device by Serial
    @GetMapping("/serial/{serial}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY','VIEWER')")
    public ResponseEntity<DeviceDto> getBySerial(@PathVariable String serial) {
        return ResponseEntity.ok(toDto(deviceService.getBySerial(serial)));
    }

    // ADMIN adds device manually (rare)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceDto> create(@RequestBody Device device) {
        return ResponseEntity.ok(toDto(deviceService.saveDevice(device)));
    }

    // Update placeholder → real serial
    @PutMapping("/{deviceId}/update-serial")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceDto> updateSerial(
            @PathVariable Long deviceId,
            @RequestParam String newSerial) {
        Device updated = deviceService.updateSerialNumber(deviceId, newSerial);
        return ResponseEntity.ok(toDto(updated));
    }
    // DeviceController.java
    @PutMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateDevice(@PathVariable Long id, @RequestBody DeviceRequest request) {
        try {
            Device updated = deviceService.updateDevice(id, request);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update device: " + e.getMessage());
        }
    }
    @PatchMapping("/{id}/notified")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateNotified(
            @PathVariable Long id,
            @RequestParam boolean value) {
        deviceService.updateNotified(id, value);
        return ResponseEntity.ok("Notified status updated");
    }

    // Convert ENTITY → DTO
    private DeviceDto toDto(Device d) {
        DeviceDto dto = new DeviceDto();
        dto.setId(d.getId());
        dto.setSerialNumber(d.getSerialNumber());
        dto.setDeviceType(d.getDeviceType());
        dto.setPoles(d.getPoles());
        dto.setEcbPresent(d.getEcbPresent());
        dto.setPlaceholder(d.getPlaceholder());
        dto.setNotified(d.getNotified() != null ? d.getNotified() : true);
        dto.setLatitude(d.getLatitude());
        dto.setLongitude(d.getLongitude());
        dto.setStatus(d.getStatus());
        dto.setLocationName(d.getLocation() != null ? d.getLocation().getName() : null);
        dto.setApproachRoad(d.getApproachRoad() != null ? d.getApproachRoad().getRoadName() : null);
        return dto;
    }
}  // ← this closing brace was missing