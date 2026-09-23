package com.inventory.msp.vms.config;

import com.inventory.msp.vms.service.VmsSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class VmsSessionInterceptor implements ClientHttpRequestInterceptor {

    private static final Pattern SERVER_ID_PATTERN = Pattern.compile("/REST/(\\d+)(?:/|$)");
    private final VmsSessionService sessionService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        Integer serverId = extractServerId(request);
        String currentSessionId = sessionService.getSessionId(serverId);
        request.getHeaders().set(HttpHeaders.COOKIE, "VSESSIONID=" + currentSessionId);

        ClientHttpResponse response = execution.execute(request, body);
        BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(response);

        if (isSessionInvalid(bufferedResponse)) {
            log.warn("VMS/TMS session expired or invalid for request to {}. Refreshing session and retrying...", request.getURI());
            sessionService.invalidateSession(serverId, currentSessionId);
            String newSessionId = sessionService.refreshSession(serverId);
            request.getHeaders().set(HttpHeaders.COOKIE, "VSESSIONID=" + newSessionId);

            ClientHttpResponse retriedResponse = execution.execute(request, body);
            return new BufferedClientHttpResponse(retriedResponse);
        }

        return bufferedResponse;
    }

    private boolean isSessionInvalid(BufferedClientHttpResponse response) throws IOException {
        // This device doesn't consistently return a JSON body on session-related
        // 401s — sometimes {"code":4000,"message":"Invalid session"}, sometimes a
        // completely empty body (confirmed via logs: "401 ... [no body]" on
        // /event/getevents for serverId=100). Since a cookie-based session either
        // works or doesn't — real credential checks only happen at login — treat
        // ANY 401 on a data-endpoint request as session-invalid and let the
        // existing refresh+retry logic in intercept() handle it, instead of only
        // matching one specific JSON shape the device doesn't always send.
        return response.getStatusCode() == HttpStatus.UNAUTHORIZED;
    }

    private Integer extractServerId(HttpRequest request) {
        Matcher matcher = SERVER_ID_PATTERN.matcher(request.getURI().getPath());
        return matcher.find()
                ? Integer.valueOf(matcher.group(1))
                : sessionService.getDefaultServerId();
    }
}
