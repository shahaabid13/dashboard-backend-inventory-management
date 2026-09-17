package com.inventory.msp.vms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDto {
    private Long eventId;
    private Integer serverId;
    private String channelId;
    private String applicationId;
    private String lpNumber;
    private Long eventTimestamp;
    private String imagePath;
    private String fileName;
    private Instant syncedAt;
}
