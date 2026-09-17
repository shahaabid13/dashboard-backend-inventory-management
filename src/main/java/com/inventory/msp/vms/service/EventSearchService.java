package com.inventory.msp.vms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.msp.vms.controller.ExternalEventCountRequest;
import com.inventory.msp.vms.dto.external.ExternalEventItemDto;
import com.inventory.msp.vms.dto.external.ExternalEventSearchRequest;
import com.inventory.msp.vms.dto.external.ExternalEventSearchResponse;
import com.inventory.msp.vms.dto.request.EventCountFilterRequest;
import com.inventory.msp.vms.dto.request.EventSearchFilterRequest;
import com.inventory.msp.vms.dto.response.EventCountResponse;
import com.inventory.msp.vms.config.VmsTmsProperties;
import com.inventory.msp.vms.entity.Event;
import com.inventory.msp.vms.entity.Server;
import com.inventory.msp.vms.exception.ResourceNotFoundException;
import com.inventory.msp.vms.repository.EventRepository;
import com.inventory.msp.vms.repository.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EventSearchService {

    private final ServerRepository serverRepository;
    private final EventRepository eventRepository;
    private final ImageStorageService imageStorageService;
    private final RestTemplate vmsRestTemplate;
    private final ObjectMapper objectMapper;
    private final VmsTmsProperties properties;
    private final EventPersistenceService eventPersistenceService;

    public EventSearchService(
            ServerRepository serverRepository,
            EventRepository eventRepository,
            ImageStorageService imageStorageService,
            @Qualifier("vmsRestTemplate") RestTemplate vmsRestTemplate,
            ObjectMapper objectMapper,
            VmsTmsProperties properties,
            EventPersistenceService eventPersistenceService) {
        this.serverRepository = serverRepository;
        this.eventRepository = eventRepository;
        this.imageStorageService = imageStorageService;
        this.vmsRestTemplate = vmsRestTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.eventPersistenceService = eventPersistenceService;
    }

    @Transactional
    public ExternalEventSearchResponse searchEvents(EventSearchFilterRequest request) {
        List<Server> servers = resolveServers(request.getServerId());
        int requestedPage = request.getPage() == null ? 1 : Math.max(1, request.getPage());
        int requestedLimit = request.getLimit() == null ? 20 : Math.max(1, request.getLimit());

        List<CompletableFuture<ServerResult<ExternalEventSearchResponse>>> futures = servers.stream()
                .map(server -> CompletableFuture.supplyAsync(() -> fetchEvents(server, request, requestedPage, requestedLimit))
                        .<ServerResult<ExternalEventSearchResponse>>handle((result, error) -> error == null
                                ? ServerResult.success(server, result)
                                : ServerResult.failure(server, error)))
                .toList();

        List<ExternalEventItemDto> mergedEvents = new ArrayList<>();
        List<Integer> failedServerIds = new ArrayList<>();
        for (CompletableFuture<ServerResult<ExternalEventSearchResponse>> future : futures) {
            ServerResult<ExternalEventSearchResponse> result = future.join();
            if (result.error() != null) {
                failedServerIds.add(result.server().getServerId());
                Throwable err = result.error();
                // Unwrap CompletionException/ExecutionException if present to reveal root cause
                Throwable root = err;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                String serverInfo = String.format("serverId=%d baseUrl=%s", result.server().getServerId(), result.server().getBaseUrl());
                log.error("External event search failed for {}: {}: {}", serverInfo, root.getClass().getName(), root.getMessage());
                // Log full stacktrace at debug level to keep error logs concise in production but available when debugging
                log.debug("Full exception for external event search failure ({}):", serverInfo, err);
                continue;
            }

            ExternalEventSearchResponse response = result.value();
            if (response != null && response.getEventlist() != null) {
                for (ExternalEventItemDto item : response.getEventlist()) {
                    String savedPath = saveEventImage(result.server(), item);
                    if (request.isPersist()) {
                        eventPersistenceService.saveEvent(result.server().getServerId(), item, savedPath);
                    }
                    mergedEvents.add(item);
                }
            }
        }

        mergedEvents.sort(Comparator.comparing(
                ExternalEventItemDto::getEventTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        int fromIndex = Math.min((requestedPage - 1) * requestedLimit, mergedEvents.size());
        int toIndex = Math.min(fromIndex + requestedLimit, mergedEvents.size());
        ExternalEventSearchResponse combined = new ExternalEventSearchResponse();
        combined.setTotalrecords(mergedEvents.size());
        combined.setTotalpages((int) Math.ceil((double) mergedEvents.size() / requestedLimit));
        combined.setCurrentpage(requestedPage);
        combined.setEventlist(mergedEvents.subList(fromIndex, toIndex));
        combined.setPartial(!failedServerIds.isEmpty());
        combined.setFailedServerIds(failedServerIds);
        return combined;
    }

    public EventCountResponse countEvents(EventCountFilterRequest request) {
        List<Server> servers = resolveServers(request.getServerId());
        List<CompletableFuture<ServerResult<Integer>>> futures = servers.stream()
                .map(server -> CompletableFuture.supplyAsync(() -> fetchEventCount(server, request))
                        .<ServerResult<Integer>>handle((result, error) -> error == null
                                ? ServerResult.success(server, result)
                                : ServerResult.failure(server, error)))
                .toList();

        int total = 0;
        List<Integer> failedServerIds = new ArrayList<>();
        for (CompletableFuture<ServerResult<Integer>> future : futures) {
            ServerResult<Integer> result = future.join();
            if (result.error() != null) {
                failedServerIds.add(result.server().getServerId());
                Throwable err = result.error();
                Throwable root = err;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                String serverInfo = String.format("serverId=%d baseUrl=%s", result.server().getServerId(), result.server().getBaseUrl());
                log.error("External event count failed for {}: {}: {}", serverInfo, root.getClass().getName(), root.getMessage());
                log.debug("Full exception for external event count failure ({}):", serverInfo, err);
            } else {
                total += result.value() == null ? 0 : result.value();
            }
        }
        return new EventCountResponse(total, !failedServerIds.isEmpty(), failedServerIds);
    }

    private List<Server> resolveServers(Integer serverId) {
        if (serverId != null) {
            return List.of(serverRepository.findById(serverId)
                    .orElseThrow(() -> new ResourceNotFoundException("Server not found with ID: " + serverId)));
        }
        return serverRepository.findByIsActiveTrue();
    }

    private ExternalEventSearchResponse fetchEvents(
            Server server,
            EventSearchFilterRequest request,
            int page,
            int limit) {
        String url = resolveEndpoint(properties.getEventSearchEndpoint(), server.getBaseUrl(), server.getServerId(), "/event/getevents");
        int externalLimit = Math.max(100, limit);
        List<ExternalEventItemDto> events = new ArrayList<>();
        int externalPage = 1;
        int totalPages;

        do {
            ExternalEventSearchRequest payload = ExternalEventSearchRequest.builder()
                    .starttimestamp(request.getStarttimestamp())
                    .endtimestamp(request.getEndtimestamp())
                    .lpnumber(request.getLpnumber())
                    .channelid(request.getChannelid())
                    .applicationid(request.getApplicationid())
                    .page(externalPage)
                    .limit(externalLimit)
                    .build();
            ResponseEntity<ExternalEventSearchResponse> response = vmsRestTemplate.postForEntity(
                    url, new HttpEntity<>(payload), ExternalEventSearchResponse.class);
            ExternalEventSearchResponse envelope = response.getBody();
            if (envelope == null) {
                break;
            }
            // Actual payload is nested inside "result[0]" per the VMS API's envelope format
            ExternalEventSearchResponse body = (envelope.getResult() != null && !envelope.getResult().isEmpty())
                    ? envelope.getResult().get(0)
                    : envelope;
            if (body == null) {
                break;
            }
            if (body.getEventlist() != null) {
                events.addAll(body.getEventlist());
            }
            totalPages = body.getTotalpages() == null ? 1 : body.getTotalpages();
            externalPage++;
        } while (externalPage <= totalPages);

        ExternalEventSearchResponse combined = new ExternalEventSearchResponse();
        combined.setTotalrecords(events.size());
        combined.setTotalpages(events.isEmpty() ? 0 : 1);
        combined.setCurrentpage(1);
        combined.setEventlist(events);
        return combined;
    }

    private Integer fetchEventCount(Server server, EventCountFilterRequest request) {
        String url = resolveEndpoint(properties.getEventCountEndpoint(), server.getBaseUrl(), server.getServerId(), "/event/count");
        ExternalEventCountRequest payload = ExternalEventCountRequest.builder()
                .starttimestamp(request.getStarttimestamp())
                .endtimestamp(request.getEndtimestamp())
                .lpnumber(request.getLpnumber())
                .channelid(request.getChannelid())
                .applicationid(request.getApplicationid())
                .page(1)
                .limit(100)
                .build();
        ResponseEntity<Map> response = vmsRestTemplate.postForEntity(
                url, new HttpEntity<>(payload), Map.class);
        Map envelope = response.getBody();
        if (envelope == null) {
            return 0;
        }

        // Actual payload is nested inside "result[0]" per the VMS API's envelope format
        Map body = envelope;
        Object resultObj = envelope.get("result");
        if (resultObj instanceof List<?> resultList && !resultList.isEmpty() && resultList.get(0) instanceof Map<?, ?> firstResult) {
            body = (Map) firstResult;
        }

        Number count = body.get("totalrecords") instanceof Number
                ? (Number) body.get("totalrecords")
                : (body.get("count") instanceof Number ? (Number) body.get("count") : null);
        return count == null ? 0 : count.intValue();
    }

    private String saveEventImage(Server server, ExternalEventItemDto item) {
        if (item.getFileBase64String() == null || item.getFileBase64String().trim().isEmpty()) {
            return null;
        }
        String savedPath = imageStorageService.saveDecodedBase64Image(
                item.getFileBase64String(), item.getName(), server.getServerId(), item.getChannelId());
        item.setFileBase64String(null);
        item.setPath(savedPath);
        return savedPath;
    }

    private String resolveEndpoint(
            String configuredEndpoint,
            String serverBaseUrl,
            Integer serverId,
            String fallbackPath) {
        if (configuredEndpoint == null || configuredEndpoint.isBlank()) {
            return String.format("%s/REST/%d%s", serverBaseUrl, serverId, fallbackPath);
        }

        return configuredEndpoint
                .replace("${vms-tms.base-url}", properties.getBaseUrl())
                .replace("${vms-tms.baseUrl}", properties.getBaseUrl())
                .replace("{serverId}", String.valueOf(serverId));
    }

    private record ServerResult<T>(Server server, T value, Throwable error) {
        static <T> ServerResult<T> success(Server server, T value) {
            return new ServerResult<>(server, value, null);
        }

        static <T> ServerResult<T> failure(Server server, Throwable error) {
            return new ServerResult<>(server, null, error);
        }
    }
}
