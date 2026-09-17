-- 1. Servers Table
CREATE TABLE IF NOT EXISTS servers (
    server_id INT PRIMARY KEY,
    server_name VARCHAR(100) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

-- Seed initial VMS and TMS servers (100 = VMS, 101 = TMS)
INSERT INTO servers (server_id, server_name, base_url, description, is_active)
VALUES
    (100, 'VMS Server', 'https://172.30.0.52:7443', 'Video Management System Server', TRUE),
    (101, 'TMS Server', 'https://172.30.0.52:7443', 'Traffic Management System Server', TRUE)
ON CONFLICT (server_id) DO NOTHING;

-- 2. Channels Table
CREATE TABLE IF NOT EXISTS channels (
    id BIGSERIAL PRIMARY KEY,
    server_id INT NOT NULL,
    channel_id VARCHAR(100) NOT NULL,
    channel_name VARCHAR(255),
    channel_ip VARCHAR(50),
    channel_type VARCHAR(100),
    snap_url VARCHAR(500),
    major_url VARCHAR(500),
    minor_url VARCHAR(500),
    analytic_url VARCHAR(500),
    username VARCHAR(100),
    password_encrypted VARCHAR(500),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    location VARCHAR(255),
    description VARCHAR(500),
    channel_mac_id VARCHAR(50),
    recording_server_id VARCHAR(100),
    recording_server_name VARCHAR(255),
    recording_stream VARCHAR(100),
    camera_installation_type VARCHAR(100),
    uuid VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_server_channel UNIQUE (server_id, channel_id),
    CONSTRAINT fk_channels_server FOREIGN KEY (server_id) REFERENCES servers(server_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_channels_server_id ON channels(server_id);
CREATE INDEX IF NOT EXISTS idx_channels_installation_type ON channels(camera_installation_type);
CREATE INDEX IF NOT EXISTS idx_channels_type ON channels(channel_type);
CREATE INDEX IF NOT EXISTS idx_channels_location ON channels(location);

-- 3. Events Table
CREATE TABLE IF NOT EXISTS events (
    event_id BIGSERIAL PRIMARY KEY,
    server_id INT NOT NULL,
    channel_id VARCHAR(100) NOT NULL,
    application_id VARCHAR(100),
    lp_number VARCHAR(50),
    event_timestamp BIGINT NOT NULL,
    image_path VARCHAR(1000),
    file_name VARCHAR(255),
    raw_response JSONB,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_events_server FOREIGN KEY (server_id) REFERENCES servers(server_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_events_server_channel ON events(server_id, channel_id);
CREATE INDEX IF NOT EXISTS idx_events_timestamp ON events(event_timestamp);
CREATE INDEX IF NOT EXISTS idx_events_lp_number ON events(lp_number);
