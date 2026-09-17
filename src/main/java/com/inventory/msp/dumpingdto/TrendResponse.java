package com.inventory.msp.dumpingdto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrendResponse {
    private String date;
    private String time;
    private Integer weight;
}
