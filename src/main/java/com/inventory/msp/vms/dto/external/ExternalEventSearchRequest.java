package com.inventory.msp.vms.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalEventSearchRequest {
    private Long starttimestamp;
    private Long endtimestamp;
    private String lpnumber;
    private String channelid;
    private String applicationid;
    private Integer page;
    private Integer limit;
}
