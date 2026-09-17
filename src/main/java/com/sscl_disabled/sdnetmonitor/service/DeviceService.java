package com.sscl.sdnetmonitor.service;

import com.sscl.sdnetmonitor.dto.DeviceDto;
import com.sscl.sdnetmonitor.entity.Device;
import com.sscl.sdnetmonitor.entity.DeviceCategory;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.Junction;
import com.sscl.sdnetmonitor.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "sdnetMonitorTransactionManager", readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public List<DeviceDto> findAll() {
        return deviceRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<DeviceDto> findByJunction(String junctionId) {
        return deviceRepository.findByJunctionId(junctionId).stream().map(this::toDto).toList();
    }

    /** @throws IllegalArgumentException (-> clean 400 via GlobalExceptionHandler)
     *  if category isn't one of DeviceCategory's values. */
    public List<DeviceDto> findByCategory(String category) {
        DeviceCategory parsed = parseCategory(category);
        return deviceRepository.findByCategory(parsed).stream().map(this::toDto).toList();
    }

    public DeviceDto findById(Long id) {
        Device d = deviceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Device not found: " + id));
        return toDto(d);
    }

    public long countByStatus(DeviceStatus status) {
        return deviceRepository.countByCurrentStatus(status);
    }

    /** For populating a category filter dropdown -- the frontend shouldn't
     *  hardcode this list separately from the enum that's authoritative. */
    public List<String> allCategories() {
        return java.util.Arrays.stream(DeviceCategory.values()).map(Enum::name).toList();
    }

    private DeviceCategory parseCategory(String raw) {
        try {
            return DeviceCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown device category '" + raw + "' -- must be one of " + allCategories());
        }
    }

    public DeviceDto toDto(Device d) {
        Junction junction = d.getJunction();
        return new DeviceDto(
                d.getId(),
                junction != null ? junction.getId() : null,
                junction != null ? junction.getName() : null,
                junction != null ? junction.getLatitude() : null,
                junction != null ? junction.getLongitude() : null,
                junction != null && junction.isHasCoordinates(),
                d.getDeviceLabel(),
                d.getIpAddress(),
                d.getCategory().name(),
                d.isNetworkSwitch(),
                d.getCurrentStatus().name(),
                d.getLastStatusChange(),
                d.getLastCheckedAt(),
                d.getLastUpAt(),
                d.isSnmpEnabled(),
                d.getLldpEnabled()
        );
    }
}
