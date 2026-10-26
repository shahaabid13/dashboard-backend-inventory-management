package com.inventory.msp.dto;

import lombok.Data;
import java.util.List;

@Data
public class CharteredBikeStationCompany {
    private String companyName;
    private String primaryColor;
    private List<CharteredBikeStation> mapStationDTOs;
}
