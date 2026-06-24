package com.flipiq.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import com.flipiq.address.dto.AddressDetails;
import com.flipiq.address.dto.AddressSuggestion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GooglePlacesServiceTest {

	@Test
	void returnsEmptyAutocompleteResultsForShortInput() {
		GooglePlacesService service = new GooglePlacesService("api-key", "https://places.test/v1", RestClient.builder());

		List<AddressSuggestion> suggestions = service.autocomplete("12", "session-123");

		assertThat(suggestions).isEmpty();
	}

	@Test
	void requiresSessionTokenForAutocomplete() {
		GooglePlacesService service = new GooglePlacesService("api-key", "https://places.test/v1", RestClient.builder());

		assertThatThrownBy(() -> service.autocomplete("1600 Pennsylvania", " "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("sessionToken is required");
	}

	@Test
	void mapsGoogleAutocompleteSuggestions() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GooglePlacesService service = new GooglePlacesService("api-key", "https://places.test/v1", builder);

		server.expect(requestTo("https://places.test/v1/places:autocomplete"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("X-Goog-Api-Key", "api-key"))
				.andRespond(withSuccess("""
						{
						  "suggestions": [
						    {
						      "placePrediction": {
						        "placeId": "ChIJabc123",
						        "text": {
						          "text": "1600 Pennsylvania Avenue NW, Washington, DC, USA"
						        }
						      }
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		List<AddressSuggestion> suggestions = service.autocomplete("1600 Pennsylvania", "session-123");

		assertThat(suggestions).containsExactly(new AddressSuggestion(
				"ChIJabc123",
				"1600 Pennsylvania Avenue NW, Washington, DC, USA"));
		server.verify();
	}

	@Test
	void mapsGooglePlaceDetails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GooglePlacesService service = new GooglePlacesService("api-key", "https://places.test/v1", builder);

		server.expect(requestTo("https://places.test/v1/places/ChIJabc123?sessionToken=session-123"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Goog-Api-Key", "api-key"))
				.andExpect(header("X-Goog-FieldMask", "id,formattedAddress,addressComponents,location"))
				.andRespond(withSuccess("""
						{
						  "id": "ChIJabc123",
						  "formattedAddress": "1600 Pennsylvania Avenue NW, Washington, DC 20500, USA",
						  "location": {
						    "latitude": 38.8977,
						    "longitude": -77.0365
						  },
						  "addressComponents": [
						    {
						      "longText": "1600",
						      "shortText": "1600",
						      "types": ["street_number"]
						    },
						    {
						      "longText": "Pennsylvania Avenue Northwest",
						      "shortText": "Pennsylvania Ave NW",
						      "types": ["route"]
						    },
						    {
						      "longText": "Washington",
						      "shortText": "Washington",
						      "types": ["locality"]
						    },
						    {
						      "longText": "District of Columbia",
						      "shortText": "DC",
						      "types": ["administrative_area_level_1"]
						    },
						    {
						      "longText": "20500",
						      "shortText": "20500",
						      "types": ["postal_code"]
						    },
						    {
						      "longText": "United States",
						      "shortText": "US",
						      "types": ["country"]
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		AddressDetails details = service.getPlaceDetails("ChIJabc123", "session-123");

		assertThat(details.formattedAddress()).isEqualTo("1600 Pennsylvania Avenue NW, Washington, DC 20500, USA");
		assertThat(details.streetNumber()).isEqualTo("1600");
		assertThat(details.route()).isEqualTo("Pennsylvania Avenue Northwest");
		assertThat(details.city()).isEqualTo("Washington");
		assertThat(details.state()).isEqualTo("District of Columbia");
		assertThat(details.stateCode()).isEqualTo("DC");
		assertThat(details.zipCode()).isEqualTo("20500");
		assertThat(details.country()).isEqualTo("United States");
		assertThat(details.latitude()).isEqualTo(38.8977);
		assertThat(details.longitude()).isEqualTo(-77.0365);
		server.verify();
	}
}
