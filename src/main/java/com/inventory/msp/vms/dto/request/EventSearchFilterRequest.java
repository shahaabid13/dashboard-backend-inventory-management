package com.inventory.msp.vms.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.msp.vms.dto.FlexibleIntegerDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchFilterRequest {

    @JsonDeserialize(using = FlexibleIntegerDeserializer.class)
    private Integer serverId;

    private Long starttimestamp;

    private Long endtimestamp;

    private String lpnumber;
    private String channelid;
    private String applicationid;

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer limit = 20;

    @Builder.Default
    private boolean persist = false;
}
