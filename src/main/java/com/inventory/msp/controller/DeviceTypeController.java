package com.inventory.msp.controller;

import com.inventory.msp.dto.DeviceTypeDto;
import com.inventory.msp.model.DeviceTypeEntity;
import com.inventory.msp.repository.DeviceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/device-types")
@RequiredArgsConstructor
public class DeviceTypeController {

    private final DeviceTypeRepository repo;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER','SUPPORT_ENGINEER','COORDINATOR','REVIEWER')")
    public ResponseEntity<List<DeviceTypeDto>> all() {
        List<DeviceTypeDto> list = repo.findByActive(true)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    private DeviceTypeDto toDto(DeviceTypeEntity e) {
        DeviceTypeDto dto = new DeviceTypeDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        return dto;
    }
}

