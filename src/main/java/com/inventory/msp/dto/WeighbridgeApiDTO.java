package com.inventory.msp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WeighbridgeApiDTO {
    private Integer slipno;
    private String vno;
    private String vname;
    private String sname;

    private Integer tweight;
    private Integer gweight;

    private String gdate;
    private String tdate;

    private Integer nweight;
    private String driver;
    private String edate;

    private String zone;
    private String mts;
    private String ward;

    @JsonProperty("wb_id")
    @JsonAlias({"wbId", "wbid"})
    private String wb_id;
}

