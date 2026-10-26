package com.inventory.msp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import com.sscl.sdnetmonitor.SdnetMonitorAutoConfiguration;


@SpringBootApplication
@Import(SdnetMonitorAutoConfiguration.class)
@EnableScheduling
@EnableAsync
public class MspApplication {

	@Bean
	public RestTemplate restTemplate() {
		// Configure timeouts and other settings for external API calls
		return new RestTemplate(clientHttpRequestFactory());
	}

	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

		// Set connection timeout to 15 seconds (15000 milliseconds)
		factory.setConnectTimeout(15000);

		// Set read timeout to 30 seconds (30000 milliseconds)
		factory.setReadTimeout(30000);


		return new BufferingClientHttpRequestFactory(factory);
	}

	public static void main(String[] args) {
		SpringApplication.run(MspApplication.class, args);

	}

}
