package com.flipiq.property;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RentCastListingsClient {

	private static final Logger log = LoggerFactory.getLogger(RentCastListingsClient.class);
	private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTINGS_RESPONSE =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient restClient;
	private final boolean apiKeyConfigured;

	public RentCastListingsClient(
			@Value("${rentcast.api-key}") String apiKey,
			@Value("${rentcast.base-url}") String baseUrl,
			RestClient.Builder restClientBuilder) {
		this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();
		log.info("RentCast listings client initialized: baseUrl={}, apiKeyConfigured={}", baseUrl, apiKeyConfigured);
		this.restClient = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeader("X-Api-Key", apiKey)
				.build();
	}

	public List<Map<String, Object>> getActiveSaleListings(String zipCode) {
		requireConfiguredApiKey();
		log.info("Calling RentCast sale listings: path=/listings/sale, zipCode={}, status=Active", zipCode);
		try {
			List<Map<String, Object>> response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/listings/sale")
							.queryParam("zipCode", zipCode)
							.queryParam("status", "Active")
							.build())
					.retrieve()
					.body(LISTINGS_RESPONSE);
			int count = response == null ? 0 : response.size();
			log.info("RentCast sale listings completed: zipCode={}, resultCount={}", zipCode, count);
			return response == null ? List.of() : response;
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				log.info("RentCast sale listings returned 404/no data: zipCode={}", zipCode);
				return List.of();
			}
			throw exception;
		}
	}

	private void requireConfiguredApiKey() {
		if (!apiKeyConfigured) {
			log.error("RentCast listings configuration failed: RENTCAST_API_KEY is not configured");
			throw new PropertyEnrichmentConfigurationException("RENTCAST_API_KEY is not configured for property enrichment.");
		}
	}
}
