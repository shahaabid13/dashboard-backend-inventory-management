package com.inventory.msp.vms.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalEventSearchResponse {
    private List<ExternalEventSearchResponse> result;
    private Integer totalrecords;
    private Integer totalpages;
    private Integer currentpage;
    private List<ExternalEventItemDto> eventlist;
    private boolean partial;
    private List<Integer> failedServerIds;
}
