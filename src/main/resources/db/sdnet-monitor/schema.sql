-- =============================================================================
-- SDNET / ICCC Device & Fibre-Link Monitoring — MySQL schema
-- Target: MySQL 8.0+
--
-- Run this once against an empty schema before starting the backend the
-- first time. application.yml uses ddl-auto=validate, so Hibernate will
-- refuse to start if the live schema and the JPA entities disagree --
-- this file is the source of truth, not Hibernate auto-generation.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS sdnet_monitor
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sdnet_monitor;

-- -----------------------------------------------------------------------------
-- junctions: every physical ICCC field location (fibre junction, router/PoP,
-- or the Data Center itself). lat/lon are nullable -- 10 locations came from
-- the device spreadsheet with no corresponding point in the fibre KMZ survey
-- yet, and need coordinates supplied before they'll render on the map.
-- -----------------------------------------------------------------------------
CREATE TABLE junctions (
    id                  VARCHAR(80)     NOT NULL,
    name                VARCHAR(150)    NOT NULL,
    type                VARCHAR(20)     NOT NULL,   -- JUNCTION | ROUTER | DATA_CENTER
    latitude            DOUBLE          NULL,
    longitude           DOUBLE          NULL,
    has_coordinates     BOOLEAN         NOT NULL DEFAULT FALSE,
    source              VARCHAR(100)    NULL,       -- e.g. 'KMZ', 'Excel-only (needs coordinates)'
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- devices: every monitorable IP endpoint -- switches, CCTV/ANPR cameras, ECBs,
-- PA speakers, UPS units, servers, storage, etc. One row per device from the
-- IP inventory spreadsheet. is_network_switch flags the device that
-- represents this junction's fibre uplink for link-status derivation.
-- -----------------------------------------------------------------------------
CREATE TABLE devices (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    junction_id         VARCHAR(80)     NOT NULL,
    device_label        VARCHAR(200)    NOT NULL,
    ip_address          VARCHAR(45)     NOT NULL,   -- v4 today; length allows v6 later
    category            VARCHAR(30)     NOT NULL,   -- backed by DeviceCategory enum: SWITCH|CCTV|PTZ|ANPR|ECB|PA|UPS|SERVER|STORAGE|LINUX|APPLICATION
    is_network_switch   BOOLEAN         NOT NULL DEFAULT FALSE,
    current_status      VARCHAR(15)     NOT NULL DEFAULT 'UNKNOWN', -- UP | DOWN | UNKNOWN
    last_status_change  TIMESTAMP       NULL,
    last_up_at          TIMESTAMP       NULL,        -- last confirmed-reachable ping, independent of status-change events; drives SLA/"down since" reporting
    last_checked_at     TIMESTAMP       NULL,
    snmp_enabled        BOOLEAN         NOT NULL DEFAULT FALSE,
    snmp_community      VARCHAR(100)    NULL,        -- populate once v2c strings are distributed; consider vault/secret storage before production use
    lldp_enabled        BOOLEAN         NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_devices_ip (ip_address),
    KEY idx_devices_junction (junction_id),
    KEY idx_devices_category (category),
    KEY idx_devices_status (current_status),
    CONSTRAINT fk_devices_junction FOREIGN KEY (junction_id) REFERENCES junctions(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- fibre_links: the 109 physical duct/fibre segments traced from the SDNET
-- KMZ survey. from/to reference junctions; confidence reflects how the
-- endpoints were resolved (geometry-only vs confirmed against the ICCC
-- topology diagram). path_geojson stores the traced polyline for the map.
-- -----------------------------------------------------------------------------
CREATE TABLE fibre_links (
    id                  VARCHAR(191)    NOT NULL,    -- e.g. 'R1', 'U6', or a descriptive KMZ name
                                                      -- (some genuinely are full descriptive phrases,
                                                      -- e.g. 'Duct from Hazratbal towards Rainawari - 1' --
                                                      -- 30 chars was too narrow for those, silently
                                                      -- never caught until seeding actually ran this far)
    display_name        VARCHAR(200)    NULL,
    from_junction_id    VARCHAR(80)     NULL,
    to_junction_id      VARCHAR(80)     NULL,
    from_confident      BOOLEAN         NOT NULL DEFAULT FALSE,
    to_confident        BOOLEAN         NOT NULL DEFAULT FALSE,
    length_meters       DOUBLE          NULL,
    confirmed           BOOLEAN         NOT NULL DEFAULT FALSE,
    diagram_confirmed   BOOLEAN         NOT NULL DEFAULT FALSE,
    current_status      VARCHAR(15)     NOT NULL DEFAULT 'UNKNOWN', -- UP | DOWN | UNKNOWN
    last_status_change  TIMESTAMP       NULL,
    last_up_at          TIMESTAMP       NULL,        -- same purpose as devices.last_up_at
    path_geojson         JSON            NULL,        -- [[lat,lon], ...] traced polyline
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_links_from (from_junction_id),
    KEY idx_links_to (to_junction_id),
    KEY idx_links_status (current_status),
    CONSTRAINT fk_links_from FOREIGN KEY (from_junction_id) REFERENCES junctions(id),
    CONSTRAINT fk_links_to   FOREIGN KEY (to_junction_id)   REFERENCES junctions(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- device_status_events: append-only log of every device status transition.
-- This is what uptime/downtime is computed from -- duration between
-- consecutive rows for the same device, not a stored running total.
-- -----------------------------------------------------------------------------
CREATE TABLE device_status_events (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    device_id           BIGINT          NOT NULL,
    previous_status     VARCHAR(15)     NULL,
    new_status          VARCHAR(15)     NOT NULL,
    changed_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    source              VARCHAR(20)     NOT NULL DEFAULT 'PING',  -- PING | SNMP | MANUAL
    note                VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    KEY idx_dse_device_time (device_id, changed_at),
    CONSTRAINT fk_dse_device FOREIGN KEY (device_id) REFERENCES devices(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- fibre_link_status_events: same idea, for links. Right now these are
-- derived (a link goes DOWN when either endpoint switch stops responding to
-- ping); once SNMP/LLDP data lands this becomes the write target for real
-- per-interface ifOperStatus transitions instead.
-- -----------------------------------------------------------------------------
CREATE TABLE fibre_link_status_events (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    fibre_link_id       VARCHAR(191)    NOT NULL,
    previous_status     VARCHAR(15)     NULL,
    new_status          VARCHAR(15)     NOT NULL,
    changed_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    source              VARCHAR(20)     NOT NULL DEFAULT 'DERIVED_PING', -- DERIVED_PING | SNMP | MANUAL
    note                VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    KEY idx_flse_link_time (fibre_link_id, changed_at),
    CONSTRAINT fk_flse_link FOREIGN KEY (fibre_link_id) REFERENCES fibre_links(id)
) ENGINE=InnoDB;
