package com.inventory.msp.vms.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalChannelDto {
    @JsonProperty("channelid")
    private String channelId;

    @JsonProperty("channelname")
    private String channelName;

    @JsonProperty("channelip")
    private String channelIp;

    @JsonProperty("channeltype")
    private String channelType;

    @JsonProperty("snapurl")
    private String snapUrl;

    @JsonProperty("majorurl")
    private String majorUrl;

    @JsonProperty("minorurl")
    private String minorUrl;

    @JsonProperty("analyticurl")
    private String analyticUrl;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("latitude")
    private BigDecimal latitude;

    @JsonProperty("longitude")
    private BigDecimal longitude;

    @JsonProperty("location")
    private String location;

    @JsonProperty("description")
    private String description;

    @JsonProperty("channelmacid")
    private String channelMacId;

    @JsonProperty("recordingserverid")
    private String recordingServerId;

    @JsonProperty("recordingservername")
    private String recordingServerName;

    @JsonProperty("recordingstream")
    private String recordingStream;

    @JsonProperty("camerainstallationtype")
    private String cameraInstallationType;

    @JsonProperty("uuid")
    private String uuid;
}
