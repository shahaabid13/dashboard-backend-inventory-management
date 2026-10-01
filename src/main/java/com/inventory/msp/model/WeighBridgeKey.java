package com.inventory.msp.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class WeighBridgeKey implements Serializable {
    @Column(name = "slipno", nullable = false)
    private Integer slipno;

    @Column(name = "wb_id", nullable = false)
    private String wbId;
}
