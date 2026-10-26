package com.inventory.msp.vms.repository;

import com.inventory.msp.vms.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {

    Optional<Channel> findByServerIdAndChannelId(Integer serverId, String channelId);

    List<Channel> findByServerId(Integer serverId);

    List<Channel> findByServerIdAndCameraInstallationTypeIgnoreCase(Integer serverId, String cameraInstallationType);

    @Query("SELECT c FROM Channel c WHERE " +
           "(:serverId IS NULL OR c.serverId = :serverId) AND " +
           "(:channelType IS NULL OR LOWER(c.channelType) = LOWER(:channelType)) AND " +
           "(:location IS NULL OR LOWER(c.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<Channel> searchChannels(
            @Param("serverId") Integer serverId,
            @Param("channelType") String channelType,
            @Param("location") String location
    );
}
