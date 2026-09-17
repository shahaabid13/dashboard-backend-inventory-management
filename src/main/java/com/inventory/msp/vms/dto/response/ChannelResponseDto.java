package com.inventory.msp.vms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponseDto {
    private Long id;
    private Integer serverId;
    private String channelId;
    private String channelName;
    private String channelIp;
    private String channelType;
    private String snapUrl;
    private String majorUrl;
    private String minorUrl;
    private String analyticUrl;
    private String username;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String location;
    private String description;
    private String channelMacId;
    private String recordingServerId;
    private String recordingServerName;
    private String recordingStream;
    private String cameraInstallationType;
    private String uuid;
    private Instant createdAt;
    private Instant updatedAt;
}
