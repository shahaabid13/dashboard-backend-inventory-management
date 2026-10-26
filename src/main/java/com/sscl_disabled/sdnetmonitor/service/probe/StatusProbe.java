package com.sscl.sdnetmonitor.service.probe;

import com.sscl.sdnetmonitor.entity.Device;
import com.sscl.sdnetmonitor.entity.DeviceStatus;

/**
 * A pluggable way to determine a device's current reachability.
 *
 * PingStatusProbe (ICMP reachability) is the only implementation wired in
 * today, because that's what's actually available: real IPs, confirmed
 * pingable, no SNMP credentials distributed yet. SnmpStatusProbe is a stub
 * for the moment v2c community strings and interface indexes land -- at
 * that point it becomes the active @Primary bean and reports real
 * per-interface ifOperStatus instead of a device-level ping result. No
 * other code in this project needs to change when that happens; everything
 * downstream depends on this interface, not on how the check is performed.
 */
public interface StatusProbe {

    DeviceStatus check(Device device);

    /** A short tag recorded on the resulting event for audit purposes, e.g. "PING" or "SNMP". */
    String sourceTag();
}
