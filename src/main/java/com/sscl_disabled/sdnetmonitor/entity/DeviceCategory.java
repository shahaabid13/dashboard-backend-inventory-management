package com.sscl.sdnetmonitor.entity;

/**
 * The 11 distinct device categories actually present in the IP inventory
 * (S.No / Junction / Device Label / IP Address / Category columns).
 *
 * Replaces what used to be a free-text String column -- which is exactly
 * how ANPR and PTZ cameras silently ended up mislabeled as CCTV during the
 * original seed import (nothing enforced that "category" could only be one
 * of a known set of values). @Enumerated(EnumType.STRING) on Device.category
 * stores/reads this the same way DeviceStatus already does elsewhere in
 * this codebase -- same VARCHAR column, no schema migration, just a
 * compile-time-checked Java type instead of an unchecked String from here on.
 */
public enum DeviceCategory {
    SWITCH,
    CCTV,
    PTZ,
    ANPR,
    ECB,
    PA,
    UPS,
    SERVER,
    STORAGE,
    LINUX,
    APPLICATION
}
