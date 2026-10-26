package com.inventory.msp.repository;


import com.inventory.msp.model.WeighBridgeEntry;
import com.inventory.msp.model.WeighBridgeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface WeighBridgeRepository extends JpaRepository<WeighBridgeEntry, WeighBridgeKey> {
    // trend: net weight over time
    @Query("SELECT e.edate, e.nweight FROM WeighBridgeEntry e WHERE e.id.wbId = :wbId ORDER BY e.edate ASC")
    List<Object[]> getNetWeightTrend(@Param("wbId") String wbId);

    // trend: gross weight over time
    @Query("SELECT e.edate, e.gweight FROM WeighBridgeEntry e WHERE e.id.wbId = :wbId ORDER BY e.edate ASC")
    List<Object[]> getGrossWeightTrend(@Param("wbId") String wbId);

    // net weight by vehicle
    @Query("SELECT e.vno, SUM(e.nweight) FROM WeighBridgeEntry e WHERE e.id.wbId = :wbId GROUP BY e.vno ORDER BY SUM(e.nweight) DESC")
    List<Object[]> getNetWeightByVehicle(@Param("wbId") String wbId);

    // last 24 hours
    @Query("SELECT e FROM WeighBridgeEntry e WHERE e.edate >= :start AND e.id.wbId = :wbId ORDER BY e.edate ASC")
    List<WeighBridgeEntry> getLast24Hours(@Param("start") String start, @Param("wbId") String wbId);

    // date range
    @Query("SELECT e FROM WeighBridgeEntry e WHERE e.edate BETWEEN :start AND :end AND e.id.wbId = :wbId ORDER BY e.edate ASC")
    List<WeighBridgeEntry> getByDateRange(@Param("start") String start,
                                          @Param("end") String end,
                                          @Param("wbId") String wbId);

    List<WeighBridgeEntry> findById_WbId(String wbId);

    // Check if an entry already exists by composite key (slipno + wbId)
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM WeighBridgeEntry e WHERE e.id.slipno = :slipno AND e.id.wbId = :wbId")
    boolean existsBySlipnoAndWbId(@Param("slipno") Integer slipno, @Param("wbId") String wbId);
}
