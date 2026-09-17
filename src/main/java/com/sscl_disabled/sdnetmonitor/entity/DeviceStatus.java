package com.sscl.sdnetmonitor.entity;

/**
 * Reachability status for a device or a derived status for a fibre link.
 *
 * UNKNOWN is a first-class state, not a placeholder for "not yet implemented" --
 * it means "no telemetry currently backs this", which is a materially different
 * operational fact from a confirmed DOWN. Never collapse the two.
 */
public enum DeviceStatus {
    UP,
    DOWN,
    UNKNOWN
}
