-- Migration: Add phone column to field_persons table
-- This migration adds the phone column needed for Coordinator callbacks

ALTER TABLE field_persons
ADD COLUMN phone VARCHAR(20) NULL AFTER role;

-- Create tickets table
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    incident_type_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    approach_road_id BIGINT NULL,
    device_type_id BIGINT NULL,
    field_person_id BIGINT NOT NULL,
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    description LONGTEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    raised_by_user_id BIGINT NOT NULL,
    coordinator_id BIGINT NULL,
    reviewer_id BIGINT NULL,
    coordinator_ack_notes VARCHAR(500) NULL,
    review_notes VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    coordinator_acked_at TIMESTAMP NULL,
    assigned_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    reopened_at TIMESTAMP NULL,
    CONSTRAINT fk_ticket_incident_type FOREIGN KEY (incident_type_id) REFERENCES incident_types(id),
    CONSTRAINT fk_ticket_location FOREIGN KEY (location_id) REFERENCES location(id),
    CONSTRAINT fk_ticket_approach_road FOREIGN KEY (approach_road_id) REFERENCES approach_road(id),
    CONSTRAINT fk_ticket_field_person FOREIGN KEY (field_person_id) REFERENCES field_persons(id),
    CONSTRAINT fk_ticket_device_type FOREIGN KEY (device_type_id) REFERENCES device_types(id),
    CONSTRAINT fk_ticket_raised_by FOREIGN KEY (raised_by_user_id) REFERENCES appuser(id),
    CONSTRAINT fk_ticket_coordinator FOREIGN KEY (coordinator_id) REFERENCES appuser(id),
    CONSTRAINT fk_ticket_reviewer FOREIGN KEY (reviewer_id) REFERENCES appuser(id)
);

-- Create ticket_history table (audit trail)
CREATE TABLE IF NOT EXISTS ticket_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    notes VARCHAR(500) NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by_user_id) REFERENCES appuser(id)
);

-- Create indexes for better performance
CREATE INDEX idx_ticket_status ON tickets(status);
CREATE INDEX idx_ticket_raised_by ON tickets(raised_by_user_id);
CREATE INDEX idx_ticket_coordinator ON tickets(coordinator_id);
CREATE INDEX idx_ticket_reviewer ON tickets(reviewer_id);
CREATE INDEX idx_ticket_device_type ON tickets(device_type_id);
CREATE INDEX idx_ticket_created_at ON tickets(created_at);
CREATE INDEX idx_history_ticket ON ticket_history(ticket_id);
CREATE INDEX idx_history_changed_by ON ticket_history(changed_by_user_id);
