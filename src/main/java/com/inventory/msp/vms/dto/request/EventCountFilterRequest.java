package com.inventory.msp.vms.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.msp.vms.dto.FlexibleIntegerDeserializer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCountFilterRequest {

    @JsonDeserialize(using = FlexibleIntegerDeserializer.class)
    private Integer serverId;

    @NotNull(message = "starttimestamp is required")
    private Long starttimestamp;

    @NotNull(message = "endtimestamp is required")
    private Long endtimestamp;

    private String lpnumber;
    private String channelid;
    private String applicationid;
}
