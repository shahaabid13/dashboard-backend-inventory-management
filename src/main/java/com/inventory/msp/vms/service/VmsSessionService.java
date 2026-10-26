package com.inventory.msp.vms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.msp.vms.config.VmsTmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class VmsSessionService {

    private final VmsTmsProperties properties;
    private final RestTemplate plainRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Integer, String> cachedSessionIds = new ConcurrentHashMap<>();
    private final Map<Integer, ReentrantLock> loginLocks = new ConcurrentHashMap<>();

    public VmsSessionService(VmsTmsProperties properties,
                             @Qualifier("plainRestTemplate") RestTemplate plainRestTemplate) {
        this.properties = properties;
        this.plainRestTemplate = plainRestTemplate;
    }

    /**
     * Returns a cached session id if available, otherwise refreshes and returns a fresh one.
     */
    public String getSessionId() {
        return getSessionId(properties.getDefaultServerId());
    }

    public String getSessionId(Integer serverId) {
        String current = cachedSessionIds.get(serverId);
        return current != null ? current : refreshSession(serverId);
    }

    public Integer getDefaultServerId() {
        return properties.getDefaultServerId();
    }

    /**
     * Forces a fresh login and updates the cache. Thread-safe: concurrent callers
     * block on the lock and double-check to reuse the winner's result.
     */
    public String refreshSession() {
        return refreshSession(properties.getDefaultServerId());
    }

    public String refreshSession(Integer serverId) {
        ReentrantLock loginLock = loginLocks.computeIfAbsent(serverId, ignored -> new ReentrantLock());
        loginLock.lock();
        try {
            String current = cachedSessionIds.get(serverId);
            if (current != null) {
                return current;
            }
            String newSession = login(serverId);
            cachedSessionIds.put(serverId, newSession);
            return newSession;
        } finally {
            loginLock.unlock();
        }
    }

    /**
     * Clears the cache only if it still matches the stale value passed in.
     */
    public void invalidateSession(String staleSessionId) {
        invalidateSession(properties.getDefaultServerId(), staleSessionId);
    }

    public void invalidateSession(Integer serverId, String staleSessionId) {
        ReentrantLock loginLock = loginLocks.computeIfAbsent(serverId, ignored -> new ReentrantLock());
        loginLock.lock();
        try {
            if (staleSessionId != null && staleSessionId.equals(cachedSessionIds.get(serverId))) {
                cachedSessionIds.remove(serverId);
            }
        } finally {
            loginLock.unlock();
        }
    }

    private String login(Integer serverId) {
        // NOTE: no /V1 prefix here — confirmed with Videonetics support that the
        // login endpoint on this deployment is /REST/user/login, not /V1/REST/user/login
        // (other endpoints like event/getevents do use /V1/, login does not).
        String loginUrl = properties.getLoginEndpoint()
                .replace("${vms-tms.base-url}", properties.getBaseUrl())
                .replace("${vms-tms.baseUrl}", properties.getBaseUrl())
                .replace("{serverId}", String.valueOf(serverId));
        if (loginUrl.isBlank()) {
            loginUrl = properties.getLoginBaseUrl() + "/REST/user/login";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Some embedded device HTTP stacks return an empty error body when the
        // Accept header isn't explicit, even though tools like Postman (which
        // send permissive default Accept headers) get a full JSON response.
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON, MediaType.ALL));

        // Confirmed with Videonetics support: the password must be sent as a
        // SHA-512 hash, not plaintext. Sending plaintext is what caused every
        // "Invalid username or password" (code 4003) rejection we saw.
        Map<String, String> body = Map.of(
                "userid", properties.getUsername(),
                "password", sha512Hex(properties.getPassword())
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        log.info("Logging in to VMS/TMS session API: url={}, userid={}, contentType={}, accept={}",
                loginUrl, properties.getUsername(), headers.getContentType(), headers.getAccept());

        try {
            ResponseEntity<String> response = plainRestTemplate.postForEntity(loginUrl, request, String.class);
            log.info("VMS login response: status={}, headers={}, bodyLength={}",
                    response.getStatusCode(), response.getHeaders(),
                    response.getBody() == null ? -1 : response.getBody().length());
            return extractSessionFromBody(response.getBody());

        } catch (HttpStatusCodeException e) {
            // Log everything we can about the failed response: status, headers,
            // and raw body, to distinguish "device rejected the login" (should have
            // a JSON body) from "something in front of the device (proxy/WAF/gateway)
            // intercepted the request" (often an empty body on a generic status code).
            String responseBody = e.getResponseBodyAsString();
            HttpHeaders errorHeaders = e.getResponseHeaders();
            log.error("VMS login failed: url={}, status={}, statusText={}, headers={}, bodyLength={}, body='{}'",
                    loginUrl,
                    e.getStatusCode(),
                    e.getStatusText(),
                    errorHeaders,
                    responseBody == null ? -1 : responseBody.length(),
                    responseBody,
                    e);
            throw new IllegalStateException(
                    "VMS login failed: status=" + e.getStatusCode()
                            + ", body=" + (responseBody == null || responseBody.isEmpty() ? "<empty>" : responseBody),
                    e);

        } catch (ResourceAccessException e) {
            // Connection-level failure: SSL handshake, timeout, connection refused, DNS, etc.
            // These never have an HTTP response body, so they should never be confused
            // with a device-level login rejection.
            log.error("VMS login failed at connection level (no HTTP response received): url={}, message={}",
                    loginUrl, e.getMessage(), e);
            throw new IllegalStateException(
                    "VMS login failed: connection-level error (" + e.getClass().getSimpleName()
                            + ") - " + e.getMessage(), e);

        } catch (RestClientException e) {
            // Catch-all for anything else the REST client can throw (e.g. message
            // conversion errors), so it's never silently mistaken for the two cases above.
            log.error("VMS login failed with unexpected RestClientException: url={}, type={}, message={}",
                    loginUrl, e.getClass().getName(), e.getMessage(), e);
            throw new IllegalStateException(
                    "VMS login failed: unexpected error (" + e.getClass().getSimpleName()
                            + ") - " + e.getMessage(), e);
        }
    }

    /**
     * Hashes the given plaintext with SHA-512 and returns it as a lowercase hex
     * string, matching the format the VMS/TMS device expects for the login
     * password field.
     */
    private String sha512Hex(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-512 is a standard JDK algorithm and always available; this
            // should be unreachable, but fail loudly rather than silently
            // sending an unhashed password if it somehow ever happens.
            throw new IllegalStateException("SHA-512 not available in this JVM", e);
        }
    }

    private String extractSessionFromBody(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode result = root.path("result");
            if (result.isArray() && result.size() > 0) {
                JsonNode first = result.get(0);
                // Try known field name variants, in order of likelihood for this deployment
                for (String field : new String[]{"vsessionid", "VSESSIONID", "SESSION", "sessionId"}) {
                    JsonNode value = first.get(field);
                    if (value != null && !value.isNull()) {
                        String sessionId = value.asText();
                        log.info("Obtained new VMS session id from field '{}'", field);
                        return sessionId;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse VMS login response body: {}", responseBody, e);
        }
        throw new IllegalStateException("VMS login response did not contain a recognizable session id. Body: " + responseBody);
    }
}