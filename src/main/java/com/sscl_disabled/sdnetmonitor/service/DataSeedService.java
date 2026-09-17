package com.sscl.sdnetmonitor.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sscl.sdnetmonitor.entity.*;
import com.sscl.sdnetmonitor.repository.DeviceRepository;
import com.sscl.sdnetmonitor.repository.FibreLinkRepository;
import com.sscl.sdnetmonitor.repository.JunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * Loads the junctions / devices / fibre-links seed data (derived from the
 * SDNET KMZ survey, the ICCC topology diagram cross-check, and the IP
 * inventory spreadsheet) into empty tables on first boot. No-ops if
 * junctions already has rows, so it's safe to leave sdnet.seed.enabled=true
 * permanently -- this only ever fires once against a fresh schema.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeedService implements CommandLineRunner {

    private final JunctionRepository junctionRepository;
    private final DeviceRepository deviceRepository;
    private final FibreLinkRepository fibreLinkRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${sdnet.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${sdnet.seed.path:classpath:seed/}")
    private String seedPath;

    @Override
    @Transactional(transactionManager = "sdnetMonitorTransactionManager")
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            log.info("Seeding disabled (sdnet.seed.enabled=false)");
            return;
        }
        if (junctionRepository.count() > 0) {
            log.info("Junctions table already has data -- skipping seed load");
            return;
        }
        log.info("Empty schema detected -- loading seed data from {}", seedPath);
        seedJunctions();
        seedDevices();
        seedFibreLinks();
        log.info("Seed load complete: {} junctions, {} devices, {} fibre links",
                junctionRepository.count(), deviceRepository.count(), fibreLinkRepository.count());
    }

    private void seedJunctions() throws Exception {
        List<SeedJunction> seeds = readList(seedPath + "junctions.json", SeedJunction.class);
        for (SeedJunction s : seeds) {
            Junction j = new Junction();
            j.setId(s.id());
            j.setName(s.name());
            j.setType(mapJunctionType(s.type()));
            j.setLatitude(s.lat());
            j.setLongitude(s.lon());
            j.setHasCoordinates(Boolean.TRUE.equals(s.hasCoordinates()));
            j.setSource(s.source());
            junctionRepository.save(j);
        }
        log.info("Loaded {} junctions", seeds.size());
    }

    private void seedDevices() throws Exception {
        List<SeedDevice> seeds = readList(seedPath + "devices.json", SeedDevice.class);
        int skipped = 0;
        for (SeedDevice s : seeds) {
            Junction junction = junctionRepository.findById(s.junctionId()).orElse(null);
            if (junction == null) {
                skipped++;
                continue;
            }
            Device d = new Device();
            d.setJunction(junction);
            d.setDeviceLabel(s.deviceLabel());
            d.setIpAddress(s.ipAddress());
            d.setCategory(s.category());
            d.setNetworkSwitch(Boolean.TRUE.equals(s.isNetworkSwitch()));
            d.setCurrentStatus(DeviceStatus.UNKNOWN);
            d.setSnmpEnabled(false);
            deviceRepository.save(d);
        }
        log.info("Loaded {} devices ({} skipped -- no matching junction)", seeds.size() - skipped, skipped);
    }

    private void seedFibreLinks() throws Exception {
        List<SeedFibreLink> seeds = readList(seedPath + "fibre_links.json", SeedFibreLink.class);
        for (SeedFibreLink s : seeds) {
            FibreLink link = new FibreLink();
            link.setId(s.id());
            link.setDisplayName(s.displayName());
            link.setFromJunction(s.fromJunctionId() != null ? junctionRepository.findById(s.fromJunctionId()).orElse(null) : null);
            link.setToJunction(s.toJunctionId() != null ? junctionRepository.findById(s.toJunctionId()).orElse(null) : null);
            link.setFromConfident(Boolean.TRUE.equals(s.fromConfident()));
            link.setToConfident(Boolean.TRUE.equals(s.toConfident()));
            link.setLengthMeters(s.lengthM());
            link.setConfirmed(Boolean.TRUE.equals(s.confirmed()));
            link.setDiagramConfirmed(Boolean.TRUE.equals(s.diagramConfirmed()));
            link.setCurrentStatus(DeviceStatus.UNKNOWN);
            link.setPathGeojson(s.path() != null ? objectMapper.writeValueAsString(s.path()) : null);
            fibreLinkRepository.save(link);
        }
        log.info("Loaded {} fibre links", seeds.size());
    }

    private JunctionType mapJunctionType(String rawType) {
        if (rawType == null) return JunctionType.JUNCTION;
        return switch (rawType.toLowerCase()) {
            case "router" -> JunctionType.ROUTER;
            case "data_center", "datacenter" -> JunctionType.DATA_CENTER;
            default -> JunctionType.JUNCTION;
        };
    }

    private <T> List<T> readList(String location, Class<T> type) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        }
    }

    // ---- seed file shapes (snake_case JSON -> camelCase records) ----

    private record SeedJunction(
            String id, String name, String type,
            Double lat, Double lon,
            @JsonProperty("has_coordinates") Boolean hasCoordinates,
            String source
    ) {}

    private record SeedDevice(
            Long id,
            @JsonProperty("junction_id") String junctionId,
            @JsonProperty("device_label") String deviceLabel,
            @JsonProperty("ip_address") String ipAddress,
            DeviceCategory category,
            @JsonProperty("is_network_switch") Boolean isNetworkSwitch
    ) {}

    private record SeedFibreLink(
            String id,
            @JsonProperty("display_name") String displayName,
            @JsonProperty("from_junction_id") String fromJunctionId,
            @JsonProperty("from_junction_name") String fromJunctionName,
            @JsonProperty("from_confident") Boolean fromConfident,
            @JsonProperty("to_junction_id") String toJunctionId,
            @JsonProperty("to_junction_name") String toJunctionName,
            @JsonProperty("to_confident") Boolean toConfident,
            @JsonProperty("length_m") Double lengthM,
            Boolean confirmed,
            @JsonProperty("diagram_confirmed") Boolean diagramConfirmed,
            List<List<Double>> path
    ) {}
}
