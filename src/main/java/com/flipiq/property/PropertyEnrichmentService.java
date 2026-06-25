package com.flipiq.property;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flipiq.address.GooglePlacesService;
import com.flipiq.address.dto.AddressDetails;
import com.flipiq.property.dto.EnrichedPropertyResponse;
import com.flipiq.property.dto.PropertyEnrichmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class PropertyEnrichmentService {

	private static final Logger log = LoggerFactory.getLogger(PropertyEnrichmentService.class);
	private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_RESPONSE =
			new ParameterizedTypeReference<>() {
			};
	private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE =
			new ParameterizedTypeReference<>() {
			};

	private final GooglePlacesService googlePlacesService;
	private final RestClient restClient;
	private final boolean apiKeyConfigured;

	public PropertyEnrichmentService(
			GooglePlacesService googlePlacesService,
			@Value("${rentcast.api-key}") String apiKey,
			@Value("${rentcast.base-url}") String baseUrl,
			RestClient.Builder restClientBuilder) {
		this.googlePlacesService = googlePlacesService;
		this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();
		log.info("RentCast property enrichment service initialized: baseUrl={}, apiKeyConfigured={}", baseUrl, apiKeyConfigured);
		this.restClient = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeader("X-Api-Key", apiKey)
				.build();
	}

	public EnrichedPropertyResponse enrich(PropertyEnrichmentRequest request) {
		requireConfiguredApiKey();
		String requestedAddress = requireAddress(request.address());
		log.info(
				"Property enrichment started: requestedAddress='{}', placeId={}, sessionTokenPresent={}",
				requestedAddress,
				request.placeId(),
				request.sessionToken() != null && !request.sessionToken().isBlank());

		AddressDetails addressDetails = getAddressDetails(request);
		String lookupAddress = firstText(
				addressDetails == null ? null : addressDetails.formattedAddress(),
				requestedAddress);

		Map<String, Object> propertyRecord = getPropertyRecord(lookupAddress);
		Map<String, Object> valueEstimate = getValueEstimate(lookupAddress);

		BigDecimal estimatedValue = firstDecimal(
				valueEstimate.get("price"),
				valueEstimate.get("value"),
				valueEstimate.get("estimatedValue"),
				propertyRecord.get("estimatedValue"),
				propertyRecord.get("value"),
				propertyRecord.get("price"));
		BigDecimal lastSalePrice = firstDecimal(propertyRecord.get("lastSalePrice"), propertyRecord.get("salePrice"));
		String lastSaleDate = normalizeDate(firstText(
				asString(propertyRecord.get("lastSaleDate")),
				asString(propertyRecord.get("saleDate"))));
		Integer livingArea = firstInteger(
				propertyRecord.get("squareFootage"),
				propertyRecord.get("livingArea"),
				propertyRecord.get("buildingArea"));

		EnrichedPropertyResponse response = new EnrichedPropertyResponse(
				firstText(addressDetails == null ? null : addressDetails.formattedAddress(), asString(propertyRecord.get("formattedAddress")), lookupAddress),
				estimatedValue,
				estimatedValue,
				lastSalePrice,
				lastSaleDate,
				firstInteger(propertyRecord.get("bedrooms"), propertyRecord.get("beds")),
				firstDecimal(propertyRecord.get("bathrooms"), propertyRecord.get("baths")),
				livingArea,
				addressDetails == null ? request.placeId() : addressDetails.placeId(),
				addressDetails == null ? lookupAddress : addressDetails.formattedAddress(),
				addressDetails == null ? null : addressDetails.streetNumber(),
				addressDetails == null ? null : addressDetails.route(),
				addressDetails == null ? null : addressDetails.city(),
				addressDetails == null ? null : addressDetails.county(),
				addressDetails == null ? null : addressDetails.state(),
				addressDetails == null ? null : addressDetails.stateCode(),
				addressDetails == null ? null : addressDetails.zipCode(),
				addressDetails == null ? null : addressDetails.country(),
				addressDetails == null ? null : addressDetails.latitude(),
				addressDetails == null ? null : addressDetails.longitude());

		log.info(
				"Property enrichment completed: lookupAddress='{}', propertyRecordKeys={}, valueEstimateKeys={}, estimatedValue={}, lastSalePrice={}, bedrooms={}, bathrooms={}, livingArea={}",
				lookupAddress,
				propertyRecord.keySet(),
				valueEstimate.keySet(),
				response.estimatedValue(),
				response.lastSalePrice(),
				response.bedrooms(),
				response.bathrooms(),
				response.livingArea());
		return response;
	}

	private AddressDetails getAddressDetails(PropertyEnrichmentRequest request) {
		if (request.placeId() == null || request.placeId().isBlank()
				|| request.sessionToken() == null || request.sessionToken().isBlank()) {
			log.info("Property enrichment skipping Google details: placeId/sessionToken not both present");
			return null;
		}

		log.info("Property enrichment requesting Google details: placeId={}, sessionToken={}", request.placeId(), request.sessionToken());
		return googlePlacesService.getPlaceDetails(request.placeId(), request.sessionToken());
	}

	private Map<String, Object> getPropertyRecord(String address) {
		log.info("Calling RentCast property record lookup: path=/properties, address='{}'", address);
		List<Map<String, Object>> response;
		try {
			response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/properties")
							.queryParam("address", address)
							.build())
					.retrieve()
					.body(LIST_RESPONSE);
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				log.info("RentCast property record lookup returned 404/no data: address='{}'", address);
				return Map.of();
			}
			throw exception;
		}

		if (response == null || response.isEmpty()) {
			log.info("RentCast property record lookup returned no results: address='{}'", address);
			return Map.of();
		}

		Map<String, Object> record = copyMap(response.getFirst());
		log.info("RentCast property record lookup completed: resultCount={}, firstRecordKeys={}", response.size(), record.keySet());
		return record;
	}

	private Map<String, Object> getValueEstimate(String address) {
		log.info("Calling RentCast value estimate lookup: path=/avm/value, address='{}'", address);
		Map<String, Object> response;
		try {
			response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/avm/value")
							.queryParam("address", address)
							.build())
					.retrieve()
					.body(MAP_RESPONSE);
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				log.info("RentCast value estimate lookup returned 404/no data: address='{}'", address);
				return Map.of();
			}
			throw exception;
		}

		Map<String, Object> estimate = response == null ? Map.of() : copyMap(response);
		log.info("RentCast value estimate lookup completed: responseKeys={}", estimate.keySet());
		return estimate;
	}

	private String requireAddress(String address) {
		if (address == null || address.isBlank()) {
			log.warn("Property enrichment validation failed: address is required");
			throw new IllegalArgumentException("address is required");
		}
		return address.trim();
	}

	private void requireConfiguredApiKey() {
		if (!apiKeyConfigured) {
			log.error("Property enrichment configuration failed: RENTCAST_API_KEY is not configured");
			throw new PropertyEnrichmentConfigurationException("RENTCAST_API_KEY is not configured for property enrichment.");
		}
	}

	private Map<String, Object> copyMap(Map<String, Object> source) {
		return source == null ? Map.of() : new HashMap<>(source);
	}

	private String firstText(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private BigDecimal firstDecimal(Object... values) {
		for (Object value : values) {
			BigDecimal decimal = asBigDecimal(value);
			if (decimal != null) {
				return decimal;
			}
		}
		return null;
	}

	private Integer firstInteger(Object... values) {
		for (Object value : values) {
			if (value instanceof Number number) {
				return number.intValue();
			}
			if (value instanceof String string && !string.isBlank()) {
				try {
					return new BigDecimal(string).intValue();
				} catch (NumberFormatException ignored) {
					// Try the next candidate.
				}
			}
		}
		return null;
	}

	private BigDecimal asBigDecimal(Object value) {
		if (value instanceof BigDecimal decimal) {
			return decimal;
		}
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		if (value instanceof String string && !string.isBlank()) {
			try {
				return new BigDecimal(string);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private String asString(Object value) {
		return value instanceof String string ? string : null;
	}

	private String normalizeDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value).toString();
		} catch (DateTimeParseException ignored) {
			try {
				return OffsetDateTime.parse(value).toLocalDate().toString();
			} catch (DateTimeParseException alsoIgnored) {
				return value;
			}
		}
	}
}
