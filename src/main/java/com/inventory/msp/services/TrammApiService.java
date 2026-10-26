package com.inventory.msp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Talks to the C-DAC TraMM legacy SOAP-over-HTTP service
 * (axis3/services/TraMMWebServicePro).
 *
 * IMPORTANT: this service does NOT return proper XML data - it returns an XML
 * envelope where the <ns:return> tag's text content is itself a raw JSON string,
 * e.g.:
 *
 *   <ns:getCorridorNamesResponse xmlns:ns="http://service">
 *     <ns:return>{"alCorridors":["CR1","CR2","CR3"]}</ns:return>
 *   </ns:getCorridorNamesResponse>
 *
 * So instead of XML-parsing the whole payload, we just slice out the text
 * between <ns:return> and </ns:return> and JSON-parse THAT.
 */
@Service
public class TrammApiService {

    private static final String RETURN_OPEN = "<ns:return>";
    private static final String RETURN_CLOSE = "</ns:return>";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tramm.base-url}")
    private String baseUrl;

    @Value("${tramm.skey}")
    private String sKey;

    public TrammApiService(RestTemplate trammRestTemplate) {
        this.restTemplate = trammRestTemplate;
    }

    /** GET /getCorridorNames -> { "alCorridors": ["CR1", "CR2", ...] } */
    public JsonNode getCorridorNames() {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getCorridorNames")
                .queryParam("sKey", sKey)
                .toUriString();
        return callAndExtractJson(url);
    }

    /** GET /getJunctionNamesForCorridor -> { "alJunctions": ["ZEROBRIDGE", ...] } */
    public JsonNode getJunctionNamesForCorridor(String corridorName) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getJunctionNamesForCorridor")
                .queryParam("sKey", sKey)
                .queryParam("corridorName", corridorName)
                .toUriString();
        return callAndExtractJson(url);
    }

    /** GET /getJunctionDetailsFromCorridor -> full junction signal-state object */
    public JsonNode getJunctionDetails(String corridorName, String junctionName) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getJunctionDetailsFromCorridor")
                .queryParam("sKey", sKey)
                .queryParam("corridorName", corridorName)
                .queryParam("junctionName", junctionName)
                .toUriString();
        return callAndExtractJson(url);
    }

    private JsonNode callAndExtractJson(String url) {
        String rawXml = restTemplate.getForObject(url, String.class);

        if (rawXml == null || rawXml.isBlank()) {
            return objectMapper.createObjectNode()
                    .put("error", "Empty response from TraMM service")
                    .put("sourceUrl", url);
        }

        int start = rawXml.indexOf(RETURN_OPEN);
        int end = rawXml.indexOf(RETURN_CLOSE);

        if (start == -1 || end == -1 || end < start) {
            // Not the shape we expected - surface the raw text so it's visible while debugging
            return objectMapper.createObjectNode()
                    .put("raw", rawXml)
                    .put("parseError", "Could not find <ns:return>...</ns:return> in response");
        }

        String innerJson = rawXml.substring(start + RETURN_OPEN.length(), end).trim();

        // Some plain numeric/error responses (e.g. "501") aren't JSON objects/arrays at all.
        // Try to parse as JSON first; if that fails, wrap the raw value so the frontend
        // still gets something usable instead of a 500.
        try {
            return objectMapper.readTree(innerJson);
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("returnValue", innerJson);
        }
    }
}