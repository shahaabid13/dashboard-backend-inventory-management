package com.inventory.msp.controller;

import com.inventory.msp.dto.DeviceDto;
import com.inventory.msp.model.Device;
import com.inventory.msp.model.DeviceType;
import com.inventory.msp.services.DeviceSearchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceSearchController {

    private static final Logger log = LoggerFactory.getLogger(DeviceSearchController.class);

    private final DeviceSearchService service;

    @GetMapping("/by-serial/{serial}")
    public ResponseEntity<DeviceDto> getBySerial(@PathVariable String serial) {
        return ResponseEntity.ok(toDto(service.getBySerial(serial)));
    }

    @GetMapping("/by-location/{locationId}")
    public ResponseEntity<List<DeviceDto>> getByLocation(@PathVariable Long locationId) {
        List<DeviceDto> list = service.getByLocation(locationId)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/by-approach/{roadId}")
    public ResponseEntity<List<DeviceDto>> getByApproach(@PathVariable Long roadId) {
        List<DeviceDto> list = service.getByApproachRoad(roadId)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<DeviceDto>> getByType(@PathVariable DeviceType type) {
        List<DeviceDto> list = service.getByType(type)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/missing")
    public ResponseEntity<List<DeviceDto>> missingSerials() {
        List<DeviceDto> list = service.getMissingSerials()
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/installed")
    public ResponseEntity<List<DeviceDto>> installedDevices() {
        List<DeviceDto> list = service.getInstalledDevices()
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DeviceDto>> searchDevices(
            @RequestParam(required = false) String locationName,
            @RequestParam(required = false) DeviceType type,
            @RequestParam(required = false) String status) {

        List<Device> devices = service.advancedSearch(locationName, type, status);
        List<DeviceDto> result = devices.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(result);
    }

    // Convert ENTITY → DTO
    private DeviceDto toDto(Device d) {
        if (log.isDebugEnabled()) {
            log.debug("[DeviceSearchController] Mapping entity -> dto id={} dbSerial='{}'", d.getId(), d.getSerialNumber());
        }
        DeviceDto dto = new DeviceDto();
        dto.setId(d.getId());
        dto.setSerialNumber(d.getSerialNumber());
        dto.setDeviceType(d.getDeviceType());
        dto.setPoles(d.getPoles());               // ← getPoles() not isPoles()
        dto.setEcbPresent(d.getEcbPresent());     // ← getEcbPresent() not isEcbPresent()
        dto.setPlaceholder(d.getPlaceholder());   // ← getPlaceholder() not isPlaceholder()
        dto.setNotified(d.getNotified());         // ← was missing entirely in this controller
        dto.setLatitude(d.getLatitude());
        dto.setLongitude(d.getLongitude());
        dto.setStatus(d.getStatus());
        dto.setLocationName(d.getLocation() != null ? d.getLocation().getName() : null);
        dto.setApproachRoad(d.getApproachRoad() != null ? d.getApproachRoad().getRoadName() : null);
        if (log.isDebugEnabled()) {
            log.debug("[DeviceSearchController] Mapped dto id={} dtoSerial='{}'", dto.getId(), dto.getSerialNumber());
        }
        return dto;
    }
}