package com.inventory.msp.task.model;

import lombok.Getter;

@Getter
public enum TaskStatus {
    OPEN("Open"),
    RESOLVED("Resolved"),
    HOLD("Hold"),
    REJECTED("Rejected");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
}
