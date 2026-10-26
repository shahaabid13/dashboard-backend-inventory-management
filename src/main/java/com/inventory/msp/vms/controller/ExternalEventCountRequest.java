package com.inventory.msp.vms.controller;

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
public class ExternalEventCountRequest {
    private Long starttimestamp;
    private Long endtimestamp;
    private String lpnumber;
    private String channelid;
    private String applicationid;
    @Builder.Default
    private Integer page = 1;
    @Builder.Default
    private Integer limit = 100;
}
