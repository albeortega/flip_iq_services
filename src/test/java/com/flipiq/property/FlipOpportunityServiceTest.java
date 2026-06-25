package com.flipiq.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FlipOpportunityServiceTest {

	@Test
	void returnsRankedFlipOpportunitiesFromRentCastListings() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RentCastListingsClient listingsClient = new RentCastListingsClient(
				"rentcast-key",
				"https://rentcast.test/v1",
				builder);
		FlipOpportunityService service = new FlipOpportunityService(listingsClient, new FlipScoreCalculator());

		server.expect(requestTo("https://rentcast.test/v1/listings/sale?zipCode=33101&status=Active"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Api-Key", "rentcast-key"))
				.andRespond(withSuccess("""
						[
						  {
						    "id": "listing-1",
						    "formattedAddress": "123 Main St, Miami, FL 33101",
						    "city": "Miami",
						    "state": "FL",
						    "zipCode": "33101",
						    "propertyType": "Single Family",
						    "price": 300000,
						    "estimatedValue": 460000,
						    "squareFootage": 1600,
						    "yearBuilt": 1988,
						    "bedrooms": 3,
						    "bathrooms": 2,
						    "latitude": 25.7617,
						    "longitude": -80.1918,
						    "daysOnMarket": 42,
						    "originalPrice": 330000
						  },
						  {
						    "id": "listing-2",
						    "formattedAddress": "456 Weak Deal Ave, Miami, FL 33101",
						    "price": 410000,
						    "estimatedValue": 430000,
						    "squareFootage": 1800,
						    "yearBuilt": 1970,
						    "daysOnMarket": 10
						  }
						]
						""", MediaType.APPLICATION_JSON));

		var response = service.search(
				"33101",
				FlipOpportunitySort.BEST_FLIP_SCORE);

		assertThat(response.zipCode()).isEqualTo("33101");
		assertThat(response.count()).isEqualTo(2);
		assertThat(response.sort()).isEqualTo("BEST_FLIP_SCORE");
		assertThat(response.properties()).hasSize(2);
		var property = response.properties().getFirst();
		assertThat(property.id()).isEqualTo("listing-1");
		assertThat(property.address()).isEqualTo("123 Main St, Miami, FL 33101");
		assertThat(property.listPrice()).isEqualByComparingTo("300000");
		assertThat(property.estimatedValue()).isEqualByComparingTo("460000");
		assertThat(property.estimatedRehabCost()).isEqualByComparingTo("72000");
		assertThat(property.closingCosts()).isEqualByComparingTo("12000");
		assertThat(property.holdingCosts()).isEqualByComparingTo("7500");
		assertThat(property.estimatedProfit()).isEqualByComparingTo("68500");
		assertThat(property.roiPercent()).isEqualByComparingTo("17.5");
		assertThat(property.discountPercent()).isEqualByComparingTo("34.8");
		assertThat(property.priceDropAmount()).isEqualByComparingTo("30000");
		assertThat(property.rehabRisk()).isEqualTo("Medium");
		assertThat(property.flipScore()).isGreaterThanOrEqualTo(70);
		assertThat(property.highlights()).contains("Estimated $68,500 profit", "Price dropped $30,000");
		server.verify();
	}

	@Test
	void requiresRentCastApiKey() {
		RentCastListingsClient listingsClient = new RentCastListingsClient(
				"",
				"https://rentcast.test/v1",
				RestClient.builder());
		FlipOpportunityService service = new FlipOpportunityService(listingsClient, new FlipScoreCalculator());

		assertThatThrownBy(() -> service.search("33101", null))
				.isInstanceOf(PropertyEnrichmentConfigurationException.class)
				.hasMessage("RENTCAST_API_KEY is not configured for property enrichment.");
	}

	@Test
	void validatesZipCode() {
		RentCastListingsClient listingsClient = new RentCastListingsClient(
				"rentcast-key",
				"https://rentcast.test/v1",
				RestClient.builder());
		FlipOpportunityService service = new FlipOpportunityService(listingsClient, new FlipScoreCalculator());

		assertThatThrownBy(() -> service.search("3310", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Please enter a valid 5-digit ZIP code.");
	}
}
