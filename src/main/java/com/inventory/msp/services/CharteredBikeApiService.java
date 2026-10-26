package com.inventory.msp.services;

import com.inventory.msp.dto.CharteredBikeLoginResponse;
import com.inventory.msp.dto.CharteredBikeStationResponse;
import com.inventory.msp.dto.CharteredBikeStation;
import com.inventory.msp.dto.CharteredBikeStationCompany;
import com.inventory.msp.model.CharteredBikeStationSnapshot;
import com.inventory.msp.repository.CharteredBikeStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharteredBikeApiService {

    private final RestTemplate restTemplate;
    private final CharteredBikeStationRepository stationRepository;

    @Value("${charteredbike.username}")
    private String username;

    @Value("${charteredbike.password}")
    private String password;

    @Value("${charteredbike.baseUrl}")
    private String baseUrl;

    private String token;
    private long tokenExpiry;
    private int loginRetryCount = 0;
    private static final int MAX_LOGIN_RETRIES = 3;

    public void login() {
        loginRetryCount = 0;
        loginWithRetry();
    }

    private void loginWithRetry() {
        try {
            String url = baseUrl + "/auth/admin-login?userName=" + username + "&password=" + password;
            log.info("Attempting Chartered Bike login (attempt {}/{})", loginRetryCount + 1, MAX_LOGIN_RETRIES);

            ResponseEntity<CharteredBikeLoginResponse> response = restTemplate.getForEntity(url, CharteredBikeLoginResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && response.getBody().getStatus() == 200) {
                this.token = response.getBody().getData().getToken();
                this.tokenExpiry = System.currentTimeMillis() + (response.getBody().getData().getTokenExpiryTime() * 1000L);
                log.info("Successfully logged in to Chartered Bike API");
                loginRetryCount = 0; // Reset counter on success
            } else {
                throw new RuntimeException("Login failed: " + (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Connection timeout or network error
            if (loginRetryCount < MAX_LOGIN_RETRIES) {
                loginRetryCount++;
                long delayMs = (long) Math.pow(2, loginRetryCount) * 1000; // Exponential backoff: 2s, 4s, 8s
                log.warn("Connection error on login attempt {}. Retrying in {}ms...", loginRetryCount, delayMs);
                try {
                    Thread.sleep(delayMs);
                    loginWithRetry();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Login retry interrupted", ie);
                }
            } else {
                log.error("Failed to login after {} attempts. Chartered Bike API may be unreachable.", MAX_LOGIN_RETRIES);
                throw new RuntimeException("Chartered Bike API connection timeout after " + MAX_LOGIN_RETRIES + " retries", e);
            }
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    public CharteredBikeStationResponse getStations() {
        CharteredBikeStationResponse response = fetchStationsFromApi();
        saveStationsToDatabase(response);
        return response;
    }

    @Transactional
    public void saveStationsToDatabase(CharteredBikeStationResponse response) {
        if (response == null || response.getData() == null) {
            log.warn("No station data to save");
            return;
        }

        List<CharteredBikeStationSnapshot> snapshots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (CharteredBikeStationCompany company : response.getData()) {
            if (company.getMapStationDTOs() != null) {
                for (CharteredBikeStation station : company.getMapStationDTOs()) {
                    CharteredBikeStationSnapshot snapshot = convertToSnapshot(station, now);
                    snapshots.add(snapshot);
                }
            }
        }

        try {
            stationRepository.saveAll(snapshots);
            log.info("Saved {} station snapshots to database", snapshots.size());
        } catch (Exception e) {
            log.error("Failed to save station snapshots to database", e);
            throw new RuntimeException("Database save failed", e);
        }
    }

    private CharteredBikeStationSnapshot convertToSnapshot(CharteredBikeStation station, LocalDateTime capturedAt) {
        CharteredBikeStationSnapshot snapshot = new CharteredBikeStationSnapshot();
        snapshot.setStationName(station.getStationName());
        snapshot.setStationNumber(station.getStationNumber());
        snapshot.setLatitude(station.getLatitude());
        snapshot.setLongitude(station.getLongitude());
        snapshot.setActive(station.isActive());
        snapshot.setBikesAvailable(station.getBikesAvailable());
        snapshot.setBikesTotal(station.getBikesTotal());
        snapshot.setBikesRack(station.getBikesRack());
        snapshot.setBikesFree(station.getBikesFree());
        snapshot.setEbikesAvailable(station.getEbikesAvailable());
        snapshot.setReportActiveBikes(station.getReportActiveBikes());
        snapshot.setReportInactiveBikes(station.getReportInactiveBikes());
        snapshot.setReportOnTripBikes(station.getReportOnTripBikes());
        snapshot.setStolenBikes(station.getStolenBikes());
        snapshot.setMissingBikes(station.getMissingBikes());
        snapshot.setBikeNumberList(station.getBikeNumberList());
        snapshot.setEcoBikeNumberList(station.getEcoBikeNumberList());
        snapshot.setEbikeNumberList(station.getEbikeNumberList());
        snapshot.setCityName(station.getCityName());
        snapshot.setCityId(station.getCityId());
        snapshot.setCapturedAt(capturedAt);
        return snapshot;
    }

    private CharteredBikeStationResponse fetchStationsFromApi() {
        if (token == null || System.currentTimeMillis() > tokenExpiry) {
            login();
        }
        String url = baseUrl + "/stations/show-stations-on-map/open?domain=asia&companyregionid=16";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<CharteredBikeStationResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, CharteredBikeStationResponse.class);
            log.info("Successfully fetched station data from API");
            return response.getBody();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Connection timeout or network error
            log.error("Connection timeout while fetching stations from Chartered Bike API: {}", e.getMessage());
            throw new RuntimeException("Chartered Bike API is unreachable. Connection timeout.", e);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                log.warn("Token expired, re-authenticating...");
                login(); // re-auth
                return fetchStationsFromApi(); // retry
            } else if (e.getMessage() != null && e.getMessage().contains("5")) {
                log.error("Server error from Chartered Bike API: {}", e.getMessage());
                throw new RuntimeException("Server error: " + e.getMessage());
            } else {
                log.error("Error fetching stations from API", e);
                throw e;
            }
        }
    }
}
