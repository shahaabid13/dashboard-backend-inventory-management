package com.inventory.msp.vms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "channels", uniqueConstraints = {
    @UniqueConstraint(name = "uk_server_channel", columnNames = {"server_id", "channel_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    @Column(name = "channel_id", nullable = false, length = 100)
    private String channelId;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "channel_ip", length = 50)
    private String channelIp;

    @Column(name = "channel_type", length = 100)
    private String channelType;

    @Column(name = "snap_url", length = 500)
    private String snapUrl;

    @Column(name = "major_url", length = 500)
    private String majorUrl;

    @Column(name = "minor_url", length = 500)
    private String minorUrl;

    @Column(name = "analytic_url", length = 500)
    private String analyticUrl;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password_encrypted", length = 500)
    private String passwordEncrypted;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "location")
    private String location;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "channel_mac_id", length = 50)
    private String channelMacId;

    @Column(name = "recording_server_id", length = 100)
    private String recordingServerId;

    @Column(name = "recording_server_name")
    private String recordingServerName;

    @Column(name = "recording_stream", length = 100)
    private String recordingStream;

    @Column(name = "camera_installation_type", length = 100)
    private String cameraInstallationType;

    @Column(name = "uuid", length = 100)
    private String uuid;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
