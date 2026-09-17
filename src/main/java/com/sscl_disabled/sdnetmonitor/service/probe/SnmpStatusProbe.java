package com.sscl.sdnetmonitor.service.probe;

import com.sscl.sdnetmonitor.entity.Device;
import com.sscl.sdnetmonitor.entity.DeviceStatus;

/**
 * NOT YET ACTIVE -- intentionally not annotated @Component.
 *
 * This is the extension point for when SNMPv2c community strings and
 * per-interface mapping (from the LLDP walk / port audit) are available.
 * At that point:
 *
 *   1. Add a real SNMP client dependency (e.g. org.snmp4j:snmp4j) to pom.xml.
 *   2. Implement check() below: walk IF-MIB::ifOperStatus
 *      (1.3.6.1.2.1.2.2.1.8.{ifIndex}) for the specific fibre-facing
 *      interface recorded against this device, using device.getSnmpCommunity().
 *      1 = UP, 2 = DOWN, anything else -> DeviceStatus.UNKNOWN.
 *   3. Annotate this class @Component and @Primary (or add a
 *      "sdnet.monitoring.probe=snmp|ping" property and pick the active bean
 *      in a @Configuration class) so it replaces PingStatusProbe as the
 *      bean MonitoringScheduler autowires.
 *   4. Nothing else in this codebase needs to change -- MonitoringScheduler,
 *      the event tables, and the uptime calculation all depend on the
 *      StatusProbe interface, not on how the check happens.
 *
 * Until then, PingStatusProbe remains the only StatusProbe bean and continues
 * to give a real (if coarser, device-level) answer for every device that's
 * confirmed pingable.
 */
public class SnmpStatusProbe implements StatusProbe {

    @Override
    public DeviceStatus check(Device device) {
        throw new UnsupportedOperationException(
                "SNMP probing is not wired in yet -- see the class-level comment for the three steps needed.");
    }

    @Override
    public String sourceTag() {
        return "SNMP";
    }
}
