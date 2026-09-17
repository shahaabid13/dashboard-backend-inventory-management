package com.sscl.sdnetmonitor.service.probe;

import com.sscl.sdnetmonitor.entity.Device;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Real, live reachability check using the system `ping` command (not
 * java.net.InetAddress.isReachable, which needs raw-socket privileges for a
 * true ICMP echo and silently falls back to a TCP connect on port 7 that
 * most of these devices don't have open -- shelling out to `ping` is the
 * more reliable choice and what most real NMS tools do).
 *
 * This tells you the device answers ICMP -- nothing more. For a switch,
 * that's a reasonable proxy for "this junction's fibre uplink is alive"
 * until per-interface SNMP data exists (see StatusProbe). For a camera/PA/
 * ECB/UPS, it's exactly the device-level answer the ICCC device inventory
 * needs.
 */
@Slf4j
@Component
public class PingStatusProbe implements StatusProbe {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    private final int timeoutSeconds;

    public PingStatusProbe(@Value("${sdnet.monitoring.ping-timeout-seconds:1}") int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        log.info("PingStatusProbe using {} ping syntax (os.name={})",
                IS_WINDOWS ? "Windows" : "Linux/Unix", System.getProperty("os.name"));
    }

    @Override
    public DeviceStatus check(Device device) {
        String ip = device.getIpAddress();
        if (ip == null || ip.isBlank()) {
            return DeviceStatus.UNKNOWN;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(buildCommand(ip));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // +1s grace beyond the OS ping tool's own timeout, for process
            // spawn/teardown overhead -- trimmed from +2s now that sweeps
            // run every 10s and every second of worst-case wait multiplies
            // across every batch in the pool.
            boolean finished = process.waitFor(timeoutSeconds + 1L, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return DeviceStatus.UNKNOWN;
            }
            return process.exitValue() == 0 ? DeviceStatus.UP : DeviceStatus.DOWN;

        } catch (IOException e) {
            log.warn("ping probe failed to execute for {} ({}): {}", device.getDeviceLabel(), ip, e.getMessage());
            return DeviceStatus.UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DeviceStatus.UNKNOWN;
        }
    }

    /**
     * Windows' ping.exe takes a completely different flag set from Linux/
     * iputils-ping -- different flag letters AND a different timeout unit
     * (milliseconds vs seconds). Getting this wrong doesn't error loudly;
     * it just makes ping.exe reject the arguments and exit non-zero, which
     * this class would silently read as every device being DOWN. Built-in
     * to Windows already -- nothing extra to install there, unlike the
     * Linux container image which needs iputils-ping added explicitly
     * (see backend/Dockerfile).
     */
    private List<String> buildCommand(String ip) {
        if (IS_WINDOWS) {
            int timeoutMs = timeoutSeconds * 1000;
            return List.of("ping", "-n", "1", "-w", String.valueOf(timeoutMs), ip);
        }
        return List.of("ping", "-c", "1", "-W", String.valueOf(timeoutSeconds), ip);
    }

    @Override
    public String sourceTag() {
        return "PING";
    }
}

