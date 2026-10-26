package com.inventory.msp.services;

import com.inventory.msp.dto.WeighbridgeApiDTO;
import com.inventory.msp.model.WeighBridgeEntry;
import com.inventory.msp.model.WeighBridgeKey;
import com.inventory.msp.repository.WeighBridgeRepository;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;



@Service
public class WeighbridgeService {

    private static final Logger log= (Logger) LoggerFactory.getLogger(WeighbridgeService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String EXTERNAL_API = "http://103.133.215.48/api/ecostan";

    private final WeighBridgeRepository repo;

    public WeighbridgeService(WeighBridgeRepository repo) {
        this.repo = repo;
    }

    public void syncWeighbridgeData() {

        WeighbridgeApiDTO[] data =
                restTemplate.getForObject(EXTERNAL_API, WeighbridgeApiDTO[].class);

        if (data == null) {
            log.info("No data found from external api WeighBridge Service");
            return;
        }

        int newRecords = 0;
        int skippedRecords = 0;
        int invalidRecords = 0;

        for (WeighbridgeApiDTO d : data) {
            String wbId = normalizeWbId(d);
            Integer slipNo = d.getSlipno();

            if (wbId == null || slipNo == null) {
                invalidRecords++;
                log.warn("Skipping weighbridge sync record with missing required data. slipNo={}, wbId={}, vehicleNo={}",
                        slipNo, d.getWb_id(), d.getVno());
                continue;
            }

            WeighBridgeKey key = new WeighBridgeKey();
            key.setSlipno(slipNo);
            key.setWbId(wbId);

            if (repo.existsBySlipnoAndWbId(slipNo, wbId)) {
                log.debug("Record with slip number {} and weighbridge ID {} already exists. Skipping...", slipNo, wbId);
                skippedRecords++;
                continue;
            }

            WeighBridgeEntry entry = new WeighBridgeEntry();
            entry.setId(key);

            entry.setVno(d.getVno());
            entry.setVname(d.getVname());
            entry.setSname(d.getSname());
            entry.setTweight(d.getTweight());
            entry.setGweight(d.getGweight());
            entry.setGdate(d.getGdate());
            entry.setTdate(d.getTdate());
            entry.setNweight(d.getNweight());
            entry.setDriver(d.getDriver());
            entry.setEdate(d.getEdate());
            entry.setZone(d.getZone());
            entry.setMts(d.getMts());
            entry.setWard(d.getWard());

            repo.save(entry);
            newRecords++;
        }

        log.info("Weighbridge data sync completed - New records inserted: {}, Existing records skipped: {}, Invalid records skipped: {}",
                newRecords, skippedRecords, invalidRecords);
    }

    private String normalizeWbId(WeighbridgeApiDTO dto) {
        if (dto == null) {
            return null;
        }

        String wbId = dto.getWb_id();
        if (wbId == null) {
            return null;
        }

        String normalized = wbId.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
