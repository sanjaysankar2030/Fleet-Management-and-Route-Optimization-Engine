package com.example.fleet.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

	// -------------------------------------------------------
	// RestTemplate with timeout settings
	// -------------------------------------------------------
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.setConnectTimeout(Duration.ofSeconds(10))
				.setReadTimeout(Duration.ofSeconds(30))
				.build();
	}

	// -------------------------------------------------------
	// ObjectMapper for JSON serialization
	// -------------------------------------------------------
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
