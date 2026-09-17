package com.sscl.sdnetmonitor.entity;

/** Where a status transition came from -- kept distinct so the UI/audit trail
 *  can tell a real ICMP result apart from a manually-forced demo state, and
 *  so SNMP-sourced data can slot in later without a schema change. */
public enum EventSource {
    PING,
    SNMP,
    DERIVED_PING,
    MANUAL
}
