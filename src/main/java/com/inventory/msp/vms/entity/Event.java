package com.inventory.msp.vms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    @Column(name = "channel_id", nullable = false, length = 100)
    private String channelId;

    @Column(name = "application_id", length = 100)
    private String applicationId;

    @Column(name = "lp_number", length = 50)
    private String lpNumber;

    @Column(name = "event_timestamp", nullable = false)
    private Long eventTimestamp;

    @Column(name = "image_path", length = 1000)
    private String imagePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @CreationTimestamp
    @Column(name = "synced_at", updatable = false)
    private Instant syncedAt;
}
