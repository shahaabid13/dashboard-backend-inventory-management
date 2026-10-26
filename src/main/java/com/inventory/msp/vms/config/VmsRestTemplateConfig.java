package com.inventory.msp.vms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class VmsRestTemplateConfig {

    private final VmsTmsProperties properties;

    /**
     * Main RestTemplate used for authenticated calls to the VMS/TMS device.
     * Attaches JSESSIONID cookie and handles automatic session refresh on 401 via VmsSessionInterceptor.
     */
    @Bean(name = "vmsRestTemplate")
    public RestTemplate vmsRestTemplate(RestTemplateBuilder builder, VmsSessionInterceptor sessionInterceptor) {
        SimpleClientHttpRequestFactory factory = buildTrustAllFactory();
        return builder
                .requestFactory(() -> factory)
                .additionalInterceptors(sessionInterceptor)
                .build();
    }

    /**
     * Plain RestTemplate with no session interceptor, used only for the
     * login call itself (to avoid circular/recursive authentication logic).
     */
    @Bean(name = "plainRestTemplate")
    public RestTemplate plainRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = buildTrustAllFactory();
        return builder
                .requestFactory(() -> factory)
                .build();
    }

    private SimpleClientHttpRequestFactory buildTrustAllFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection httpsConnection && properties.isTrustSelfSignedCert()) {
                    try {
                        TrustManager[] trustAllCerts = new TrustManager[]{
                                new X509TrustManager() {
                                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                                }
                        };
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, trustAllCerts, new SecureRandom());
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    } catch (Exception e) {
                        log.error("Failed to configure SSL for RestTemplate: {}", e.getMessage());
                    }
                }
                super.prepareConnection(connection, httpMethod);
            }
        };

        factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        return factory;
    }
}
