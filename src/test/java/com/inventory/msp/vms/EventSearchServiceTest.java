package com.inventory.msp.vms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.msp.vms.config.VmsTmsProperties;
import com.inventory.msp.vms.dto.external.ExternalEventItemDto;
import com.inventory.msp.vms.dto.external.ExternalEventSearchResponse;
import com.inventory.msp.vms.dto.request.EventCountFilterRequest;
import com.inventory.msp.vms.dto.request.EventSearchFilterRequest;
import com.inventory.msp.vms.dto.response.EventCountResponse;
import com.inventory.msp.vms.entity.Server;
import com.inventory.msp.vms.repository.EventRepository;
import com.inventory.msp.vms.repository.ServerRepository;
import com.inventory.msp.vms.service.EventPersistenceService;
import com.inventory.msp.vms.service.EventSearchService;
import com.inventory.msp.vms.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSearchServiceTest {

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private RestTemplate vmsRestTemplate;

    @Mock
    private EventPersistenceService eventPersistenceService;

    private EventSearchService eventSearchService;
    private Server server100;
    private Server server101;

    @BeforeEach
    void setUp() {
        VmsTmsProperties properties = new VmsTmsProperties();
        properties.setBaseUrl("https://vms/REST/{serverId}");
        properties.setEventCountEndpoint("${vms-tms.base-url}/event/count");
        properties.setEventSearchEndpoint("${vms-tms.base-url}/event/getevents");

        eventSearchService = new EventSearchService(
                serverRepository,
                eventRepository,
                imageStorageService,
                vmsRestTemplate,
                new ObjectMapper(),
                properties,
                eventPersistenceService
        );

        server100 = server(100);
        server101 = server(101);
        when(serverRepository.findByIsActiveTrue()).thenReturn(List.of(server100, server101));
    }

    @Test
    void countAllServersSumsSuccessfulResponses() {
        when(vmsRestTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    return ResponseEntity.ok(Map.of("totalrecords", url.contains("/100/") ? 3 : 7));
                });

        EventCountResponse response = eventSearchService.countEvents(countRequest(null));

        assertEquals(10, response.getCount());
        assertFalse(response.isPartial());
        assertTrue(response.getFailedServerIds().isEmpty());
    }

    @Test
    void searchAllServersMergesAndSortsEvents() {
        when(vmsRestTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(ExternalEventSearchResponse.class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    return ResponseEntity.ok(searchResponse(url.contains("/100/") ? 1000L : 3000L));
                });

        ExternalEventSearchResponse response = eventSearchService.searchEvents(searchRequest(null, 1, 10));

        assertEquals(2, response.getTotalrecords());
        assertEquals(3000L, response.getEventlist().get(0).getEventTimestamp());
        assertFalse(response.isPartial());
    }

    @Test
    void oneServerDownReturnsPartialCount() {
        when(vmsRestTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    if (url.contains("/101/")) {
                        throw new RestClientException("server unavailable");
                    }
                    return ResponseEntity.ok(Map.of("totalrecords", 4));
                });

        EventCountResponse response = eventSearchService.countEvents(countRequest(null));

        assertEquals(4, response.getCount());
        assertTrue(response.isPartial());
        assertEquals(List.of(101), response.getFailedServerIds());
    }

    @Test
    void bothServersDownReturnEmptyPartialResults() {
        when(vmsRestTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("servers unavailable"));

        EventCountResponse response = eventSearchService.countEvents(countRequest(null));

        assertEquals(0, response.getCount());
        assertTrue(response.isPartial());
        assertEquals(List.of(100, 101), response.getFailedServerIds());
    }

    @Test
    void emptyDateRangeReturnsEmptySuccessfulResults() {
        when(vmsRestTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("totalrecords", 0)));

        EventCountResponse response = eventSearchService.countEvents(countRequest(null));

        assertEquals(0, response.getCount());
        assertFalse(response.isPartial());
    }

    private EventCountFilterRequest countRequest(Integer serverId) {
        return EventCountFilterRequest.builder()
                .serverId(serverId)
                .starttimestamp(100L)
                .endtimestamp(200L)
                .build();
    }

    private EventSearchFilterRequest searchRequest(Integer serverId, int page, int limit) {
        return EventSearchFilterRequest.builder()
                .serverId(serverId)
                .starttimestamp(100L)
                .endtimestamp(200L)
                .page(page)
                .limit(limit)
                .persist(false)
                .build();
    }

    private ExternalEventSearchResponse searchResponse(long timestamp) {
        ExternalEventItemDto item = new ExternalEventItemDto();
        item.setEventTimestamp(timestamp);

        ExternalEventSearchResponse response = new ExternalEventSearchResponse();
        response.setTotalrecords(1);
        response.setEventlist(List.of(item));
        return response;
    }

    private Server server(int serverId) {
        return Server.builder()
                .serverId(serverId)
                .serverName("Server " + serverId)
                .baseUrl("https://vms")
                .isActive(true)
                .build();
    }
}
