package com.inventory.msp.vms.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalEventItemDto {

    // --- Fields confirmed present in the real device response (via Postman) ---
    // Added so they're no longer silently dropped by Jackson during deserialization.

    @JsonProperty("eventid")
    private Long eventId;

    @JsonProperty("alerttype")
    private Integer alertType;

    @JsonProperty("alertname")
    private String alertName;

    @JsonProperty("channelname")
    private String channelName;

    @JsonProperty("eventlocation")
    private String eventLocation;

    @JsonProperty("eventtime")
    private String eventTime;

    @JsonProperty("message")
    private String message;

    @JsonProperty("action")
    private String action;

    @JsonProperty("clipurl")
    private String clipUrl;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("sender")
    private String sender;

    // --- Existing fields, unchanged — used by EventSearchService's
    // evidence-image-saving logic (saveDecodedBase64Image / saveEventLocally).
    // Left exactly as they were so that flow keeps working. ---

    @JsonProperty("channelid")
    private String channelId;

    @JsonProperty("applicationid")
    private String applicationId;

    @JsonProperty("lpnumber")
    @JsonAlias({"numberplate", "numberPlate"})
    private String lpNumber;

    @JsonProperty("eventtimestamp")
    private Long eventTimestamp;

    @JsonProperty("fileBase64String")
    private String fileBase64String;

    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;
}