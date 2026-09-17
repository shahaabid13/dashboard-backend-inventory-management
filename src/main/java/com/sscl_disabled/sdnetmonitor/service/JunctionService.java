package com.sscl.sdnetmonitor.service;

import com.sscl.sdnetmonitor.dto.JunctionDto;
import com.sscl.sdnetmonitor.entity.Junction;
import com.sscl.sdnetmonitor.repository.DeviceRepository;
import com.sscl.sdnetmonitor.repository.FibreLinkRepository;
import com.sscl.sdnetmonitor.repository.JunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "sdnetMonitorTransactionManager", readOnly = true)
public class JunctionService {

    private final JunctionRepository junctionRepository;
    private final DeviceRepository deviceRepository;
    private final FibreLinkRepository fibreLinkRepository;

    public List<JunctionDto> findAll() {
        return junctionRepository.findAll().stream().map(this::toDto).toList();
    }

    public JunctionDto findById(String id) {
        Junction j = junctionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Junction not found: " + id));
        return toDto(j);
    }

    private JunctionDto toDto(Junction j) {
        long deviceCount = deviceRepository.findByJunctionId(j.getId()).size();
        long linkCount = fibreLinkRepository.findByFromJunctionIdOrToJunctionId(j.getId(), j.getId()).size();
        return new JunctionDto(
                j.getId(), j.getName(), j.getType().name(),
                j.getLatitude(), j.getLongitude(), j.isHasCoordinates(), j.getSource(),
                deviceCount, linkCount
        );
    }
}
