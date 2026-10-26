package com.inventory.msp.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "weighbridge_entries")
public class WeighBridgeEntry {

    @EmbeddedId
    private WeighBridgeKey id;

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
}
