package com.inventory.msp.vms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EventCountResponse {
    private int count;
    private boolean partial;
    private List<Integer> failedServerIds;
}
