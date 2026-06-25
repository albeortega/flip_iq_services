package com.flipiq.property;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.flipiq.property.dto.EnrichedPropertyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PropertyEnrichmentController.class)
class PropertyEnrichmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PropertyEnrichmentService propertyEnrichmentService;

	@Test
	void enrichesPropertyFromAddress() throws Exception {
		when(propertyEnrichmentService.enrich(any())).thenReturn(new EnrichedPropertyResponse(
				"123 Main St, Miami, FL 33101",
				new BigDecimal("615000"),
				new BigDecimal("615000"),
				new BigDecimal("480000"),
				"2022-08-12",
				4,
				new BigDecimal("3"),
				2420,
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

		mockMvc.perform(post("/api/properties/enrich")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "address": "123 Main St, Miami, FL 33101",
								  "placeId": "ChIJabc123",
								  "sessionToken": "session-123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").value("123 Main St, Miami, FL 33101"))
				.andExpect(jsonPath("$.estimatedValue").value(615000))
				.andExpect(jsonPath("$.zestimate").value(615000))
				.andExpect(jsonPath("$.lastSalePrice").value(480000))
				.andExpect(jsonPath("$.lastSaleDate").value("2022-08-12"))
				.andExpect(jsonPath("$.bedrooms").value(4))
				.andExpect(jsonPath("$.bathrooms").value(3))
				.andExpect(jsonPath("$.livingArea").value(2420));
	}
}
