package com.flipiq.property;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RentCastListingsClient {

	private static final Logger log = LoggerFactory.getLogger(RentCastListingsClient.class);
	private static final int LOG_CHUNK_SIZE = 6000;
	private static final TypeReference<List<Map<String, Object>>> LISTINGS_RESPONSE = new TypeReference<>() {
	};
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
			String rawResponse = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/listings/sale")
							.queryParam("zipCode", zipCode)
							.queryParam("status", "Active")
							.build())
					.retrieve()
					.body(String.class);
			logRawResponse(zipCode, rawResponse);
			List<Map<String, Object>> response = parseListings(rawResponse);
			int count = response == null ? 0 : response.size();
			log.info("RentCast sale listings completed: zipCode={}, resultCount={}", zipCode, count);
			if (response != null && !response.isEmpty()) {
				log.info("RentCast sale listings first result keys: zipCode={}, keys={}", zipCode, response.getFirst().keySet());
				response.stream()
						.limit(10)
						.forEach(listing -> log.info(
								"RentCast sale listings raw item JSON: zipCode={}, rawItem={}",
								zipCode,
								toJson(listing)));
			}
			return response == null ? List.of() : response;
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				log.info("RentCast sale listings returned 404/no data: zipCode={}", zipCode);
				return List.of();
			}
			throw exception;
		}
	}

	private void logRawResponse(String zipCode, String rawResponse) {
		if (rawResponse == null) {
			log.info("RentCast sale listings raw response is null: zipCode={}", zipCode);
			return;
		}

		int totalLength = rawResponse.length();
		int totalChunks = Math.max(1, (int) Math.ceil(totalLength / (double) LOG_CHUNK_SIZE));
		log.info(
				"RentCast sale listings raw response metadata: zipCode={}, length={}, chunkSize={}, totalChunks={}",
				zipCode,
				totalLength,
				LOG_CHUNK_SIZE,
				totalChunks);
		for (int index = 0; index < totalChunks; index++) {
			int start = index * LOG_CHUNK_SIZE;
			int end = Math.min(start + LOG_CHUNK_SIZE, totalLength);
			log.info(
					"RentCast sale listings raw response chunk: zipCode={}, chunk={}/{}, range={}..{}, body={}",
					zipCode,
					index + 1,
					totalChunks,
					start,
					end,
					rawResponse.substring(start, end));
		}
	}

	private List<Map<String, Object>> parseListings(String rawResponse) {
		if (rawResponse == null || rawResponse.isBlank()) {
			return List.of();
		}
		try {
			return OBJECT_MAPPER.readValue(rawResponse, LISTINGS_RESPONSE);
		} catch (JsonProcessingException exception) {
			log.error("Could not parse RentCast listings response: {}", exception.getMessage());
			throw new IllegalStateException("Could not parse RentCast listings response.", exception);
		}
	}

	private void requireConfiguredApiKey() {
		if (!apiKeyConfigured) {
			log.error("RentCast listings configuration failed: RENTCAST_API_KEY is not configured");
			throw new PropertyEnrichmentConfigurationException("RENTCAST_API_KEY is not configured for property enrichment.");
		}
	}

	private String toJson(Object value) {
		try {
			return OBJECT_MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			log.warn("Could not serialize RentCast response for logging: {}", exception.getMessage());
			return String.valueOf(value);
		}
	}
}
