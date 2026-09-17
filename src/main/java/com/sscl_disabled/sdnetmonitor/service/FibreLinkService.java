package com.sscl.sdnetmonitor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sscl.sdnetmonitor.dto.FibreLinkDto;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.FibreLink;
import com.sscl.sdnetmonitor.entity.Junction;
import com.sscl.sdnetmonitor.repository.FibreLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "sdnetMonitorTransactionManager", readOnly = true)
public class FibreLinkService {

    private final FibreLinkRepository fibreLinkRepository;
    private final ObjectMapper objectMapper;

    public List<FibreLinkDto> findAll() {
        return fibreLinkRepository.findAll().stream().map(this::toDto).toList();
    }

    public FibreLinkDto findById(String id) {
        FibreLink link = fibreLinkRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fibre link not found: " + id));
        return toDto(link);
    }

    public List<FibreLinkDto> findByJunction(String junctionId) {
        return fibreLinkRepository.findByFromJunctionIdOrToJunctionId(junctionId, junctionId)
                .stream().map(this::toDto).toList();
    }

    public long countByStatus(DeviceStatus status) {
        return fibreLinkRepository.countByCurrentStatus(status);
    }

    public double totalLengthKm() {
        return fibreLinkRepository.findAll().stream()
                .mapToDouble(l -> l.getLengthMeters() != null ? l.getLengthMeters() : 0.0)
                .sum() / 1000.0;
    }

    public FibreLinkDto toDto(FibreLink link) {
        Junction from = link.getFromJunction();
        Junction to = link.getToJunction();
        return new FibreLinkDto(
                link.getId(),
                link.getDisplayName(),
                from != null ? from.getId() : null,
                from != null ? from.getName() : null,
                link.isFromConfident(),
                to != null ? to.getId() : null,
                to != null ? to.getName() : null,
                link.isToConfident(),
                link.getLengthMeters(),
                link.isConfirmed(),
                link.isDiagramConfirmed(),
                link.getCurrentStatus().name(),
                link.getLastStatusChange(),
                parsePath(link.getPathGeojson())
        );
    }

    private List<List<Double>> parsePath(String pathJson) {
        if (pathJson == null || pathJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(pathJson, new TypeReference<List<List<Double>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse path_geojson, returning empty path: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
