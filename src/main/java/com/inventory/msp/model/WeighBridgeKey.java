package com.inventory.msp.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class WeighBridgeKey implements Serializable {
    private Integer slipno;

    @Column(name = "wb_id")
    private String wbId;
}
