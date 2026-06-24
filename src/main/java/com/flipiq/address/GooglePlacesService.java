package com.flipiq.address;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flipiq.address.dto.AddressDetails;
import com.flipiq.address.dto.AddressSuggestion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GooglePlacesService {

	private static final String DETAILS_FIELD_MASK = "id,formattedAddress,addressComponents,location";
	private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE =
			new ParameterizedTypeReference<>() {
			};

	private final RestClient restClient;

	public GooglePlacesService(
			@Value("${google.maps.api-key}") String apiKey,
			@Value("${google.maps.places-base-url}") String placesBaseUrl,
			RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder
				.baseUrl(placesBaseUrl)
				.defaultHeader("X-Goog-Api-Key", apiKey)
				.build();
	}

	public List<AddressSuggestion> autocomplete(String input, String sessionToken) {
		String trimmedInput = input == null ? "" : input.trim();
		if (trimmedInput.length() < 3) {
			return List.of();
		}

		requireText(sessionToken, "sessionToken");

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("input", trimmedInput);
		requestBody.put("sessionToken", sessionToken);
		requestBody.put("includedRegionCodes", List.of("us"));
		requestBody.put("includedPrimaryTypes", List.of("street_address", "premise", "subpremise"));

		Map<String, Object> response = restClient.post()
				.uri("/places:autocomplete")
				.contentType(MediaType.APPLICATION_JSON)
				.body(requestBody)
				.retrieve()
				.body(MAP_RESPONSE);

		return toSuggestions(response);
	}

	public AddressDetails getPlaceDetails(String placeId, String sessionToken) {
		requireText(placeId, "placeId");
		requireText(sessionToken, "sessionToken");

		Map<String, Object> response = restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/places/{placeId}")
						.queryParam("sessionToken", sessionToken)
						.build(placeId))
				.header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
				.retrieve()
				.body(MAP_RESPONSE);

		if (response == null) {
			throw new IllegalStateException("No response from Google Place Details API");
		}

		Map<String, String> parsed = parseAddressComponents(asListOfMaps(response.get("addressComponents")));
		Map<String, Object> location = asMap(response.get("location"));

		return new AddressDetails(
				asString(response.get("id")),
				asString(response.get("formattedAddress")),
				parsed.get("street_number"),
				parsed.get("route"),
				parsed.get("locality"),
				parsed.get("administrative_area_level_2"),
				parsed.get("administrative_area_level_1"),
				parsed.get("state_code"),
				parsed.get("postal_code"),
				parsed.get("country"),
				asDouble(location.get("latitude")),
				asDouble(location.get("longitude")));
	}

	private List<AddressSuggestion> toSuggestions(Map<String, Object> response) {
		if (response == null) {
			return List.of();
		}

		List<AddressSuggestion> results = new ArrayList<>();
		for (Map<String, Object> suggestion : asListOfMaps(response.get("suggestions"))) {
			Map<String, Object> placePrediction = asMap(suggestion.get("placePrediction"));
			Map<String, Object> text = asMap(placePrediction.get("text"));
			String placeId = asString(placePrediction.get("placeId"));
			String description = asString(text.get("text"));

			if (placeId != null && description != null) {
				results.add(new AddressSuggestion(placeId, description));
			}
		}

		return results;
	}

	private Map<String, String> parseAddressComponents(List<Map<String, Object>> components) {
		Map<String, String> result = new HashMap<>();

		for (Map<String, Object> component : components) {
			String longText = asString(component.get("longText"));
			String shortText = asString(component.get("shortText"));

			for (Object typeValue : asList(component.get("types"))) {
				if (!(typeValue instanceof String type)) {
					continue;
				}

				switch (type) {
					case "street_number" -> result.put("street_number", longText);
					case "route" -> result.put("route", longText);
					case "locality" -> result.put("locality", longText);
					case "postal_town", "sublocality" -> result.putIfAbsent("locality", longText);
					case "administrative_area_level_2" -> result.put("administrative_area_level_2", longText);
					case "administrative_area_level_1" -> {
						result.put("administrative_area_level_1", longText);
						result.put("state_code", shortText);
					}
					case "postal_code" -> result.put("postal_code", longText);
					case "country" -> result.put("country", longText);
					default -> {
					}
				}
			}
		}

		return result;
	}

	private void requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("%s is required".formatted(fieldName));
		}
	}

	private List<Map<String, Object>> asListOfMaps(Object value) {
		List<Map<String, Object>> maps = new ArrayList<>();
		for (Object item : asList(value)) {
			if (item instanceof Map<?, ?> rawMap) {
				Map<String, Object> map = new HashMap<>();
				rawMap.forEach((key, mapValue) -> {
					if (key instanceof String stringKey) {
						map.put(stringKey, mapValue);
					}
				});
				maps.add(map);
			}
		}
		return maps;
	}

	private List<?> asList(Object value) {
		return value instanceof List<?> list ? list : List.of();
	}

	private Map<String, Object> asMap(Object value) {
		if (!(value instanceof Map<?, ?> rawMap)) {
			return Map.of();
		}

		Map<String, Object> map = new HashMap<>();
		rawMap.forEach((key, mapValue) -> {
			if (key instanceof String stringKey) {
				map.put(stringKey, mapValue);
			}
		});
		return map;
	}

	private String asString(Object value) {
		return value instanceof String string ? string : null;
	}

	private Double asDouble(Object value) {
		return value instanceof Number number ? number.doubleValue() : null;
	}
}
