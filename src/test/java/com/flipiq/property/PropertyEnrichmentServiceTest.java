package com.flipiq.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.flipiq.address.GooglePlacesService;
import com.flipiq.address.dto.AddressDetails;
import com.flipiq.property.dto.EnrichedPropertyResponse;
import com.flipiq.property.dto.PropertyEnrichmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PropertyEnrichmentServiceTest {

	@Test
	void enrichesPropertyFromRentCastAndGoogleDetails() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GooglePlacesService googlePlacesService = mock(GooglePlacesService.class);
		PropertyEnrichmentService service = new PropertyEnrichmentService(
				googlePlacesService,
				"rentcast-key",
				"https://rentcast.test/v1",
				builder);

		when(googlePlacesService.getPlaceDetails("ChIJabc123", "session-123")).thenReturn(new AddressDetails(
				"ChIJabc123",
				"123 Main St, Miami, FL 33101, USA",
				"123",
				"Main St",
				"Miami",
				"Miami-Dade County",
				"Florida",
				"FL",
				"33101",
				"United States",
				25.761681,
				-80.191788));

		server.expect(requestTo("https://rentcast.test/v1/properties?address=123%20Main%20St,%20Miami,%20FL%2033101,%20USA"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Api-Key", "rentcast-key"))
				.andRespond(withSuccess("""
						[
						  {
						    "formattedAddress": "123 Main St, Miami, FL 33101",
						    "lastSalePrice": 480000,
						    "lastSaleDate": "2022-08-12T00:00:00Z",
						    "bedrooms": 4,
						    "bathrooms": 3,
						    "squareFootage": 2420
						  }
						]
						""", MediaType.APPLICATION_JSON));

		server.expect(requestTo("https://rentcast.test/v1/avm/value?address=123%20Main%20St,%20Miami,%20FL%2033101,%20USA"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Api-Key", "rentcast-key"))
				.andRespond(withSuccess("""
						{
						  "price": 615000
						}
						""", MediaType.APPLICATION_JSON));

		EnrichedPropertyResponse response = service.enrich(new PropertyEnrichmentRequest(
				"123 Main St, Miami, FL 33101",
				"ChIJabc123",
				"session-123"));

		assertThat(response.address()).isEqualTo("123 Main St, Miami, FL 33101, USA");
		assertThat(response.estimatedValue()).isEqualByComparingTo("615000");
		assertThat(response.zestimate()).isEqualByComparingTo("615000");
		assertThat(response.lastSalePrice()).isEqualByComparingTo("480000");
		assertThat(response.lastSaleDate()).isEqualTo("2022-08-12");
		assertThat(response.bedrooms()).isEqualTo(4);
		assertThat(response.bathrooms()).isEqualByComparingTo("3");
		assertThat(response.livingArea()).isEqualTo(2420);
		assertThat(response.city()).isEqualTo("Miami");
		assertThat(response.stateCode()).isEqualTo("FL");
		server.verify();
	}

	@Test
	void requiresRentCastApiKey() {
		PropertyEnrichmentService service = new PropertyEnrichmentService(
				mock(GooglePlacesService.class),
				"",
				"https://rentcast.test/v1",
				RestClient.builder());

		assertThatThrownBy(() -> service.enrich(new PropertyEnrichmentRequest("123 Main St", null, null)))
				.isInstanceOf(PropertyEnrichmentConfigurationException.class)
				.hasMessage("RENTCAST_API_KEY is not configured for property enrichment.");
	}
}
