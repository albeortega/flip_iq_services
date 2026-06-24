package com.flipiq.address;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.flipiq.address.dto.AddressDetails;
import com.flipiq.address.dto.AddressSuggestion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AddressController.class)
class AddressControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GooglePlacesService googlePlacesService;

	@Test
	void returnsAutocompleteSuggestions() throws Exception {
		when(googlePlacesService.autocomplete("1600 Pennsylvania", "session-123"))
				.thenReturn(List.of(new AddressSuggestion("ChIJabc123", "1600 Pennsylvania Avenue NW, Washington, DC, USA")));

		mockMvc.perform(get("/api/addresses/autocomplete")
						.param("input", "1600 Pennsylvania")
						.param("sessionToken", "session-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].placeId").value("ChIJabc123"))
				.andExpect(jsonPath("$[0].description").value("1600 Pennsylvania Avenue NW, Washington, DC, USA"));
	}

	@Test
	void returnsAddressDetails() throws Exception {
		when(googlePlacesService.getPlaceDetails("ChIJabc123", "session-123"))
				.thenReturn(new AddressDetails(
						"ChIJabc123",
						"1600 Pennsylvania Avenue NW, Washington, DC 20500, USA",
						"1600",
						"Pennsylvania Avenue Northwest",
						"Washington",
						"District of Columbia",
						"District of Columbia",
						"DC",
						"20500",
						"United States",
						38.8977,
						-77.0365));

		mockMvc.perform(get("/api/addresses/details")
						.param("placeId", "ChIJabc123")
						.param("sessionToken", "session-123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formattedAddress").value("1600 Pennsylvania Avenue NW, Washington, DC 20500, USA"))
				.andExpect(jsonPath("$.city").value("Washington"))
				.andExpect(jsonPath("$.stateCode").value("DC"))
				.andExpect(jsonPath("$.latitude").value(38.8977))
				.andExpect(jsonPath("$.longitude").value(-77.0365));
	}

	@Test
	void rejectsMissingSessionToken() throws Exception {
		mockMvc.perform(get("/api/addresses/autocomplete")
						.param("input", "1600 Pennsylvania"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Required request parameter 'sessionToken' for method parameter type String is not present"));
	}
}
