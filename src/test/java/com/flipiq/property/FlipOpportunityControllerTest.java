package com.flipiq.property;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.flipiq.property.dto.FlipOpportunityPropertyDto;
import com.flipiq.property.dto.FlipOpportunityResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FlipOpportunityController.class)
class FlipOpportunityControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FlipOpportunityService flipOpportunityService;

	@Test
	void returnsFlipOpportunitiesForZipCode() throws Exception {
		when(flipOpportunityService.search(
				eq("33101"),
				eq(FlipOpportunitySort.HIGHEST_PROFIT)))
				.thenReturn(new FlipOpportunityResponse(
						"33101",
						1,
						"HIGHEST_PROFIT",
						List.of(new FlipOpportunityPropertyDto(
								"listing-1",
								"123 Main St, Miami, FL 33101",
								"Miami",
								"FL",
								"33101",
								"Single Family",
								new BigDecimal("300000"),
								new BigDecimal("460000"),
								new BigDecimal("72000"),
								new BigDecimal("12000"),
								new BigDecimal("7500"),
								new BigDecimal("68500"),
								new BigDecimal("17.5"),
								new BigDecimal("34.8"),
								new BigDecimal("30000"),
								42,
								"Medium",
								3,
								new BigDecimal("2"),
								1600,
								1988,
								25.7617,
								-80.1918,
								82,
								"Strong Flip Candidate",
								List.of("Estimated $68,500 profit")))));

		mockMvc.perform(get("/api/properties/flip-opportunities")
						.queryParam("zipCode", "33101")
						.queryParam("sort", "HIGHEST_PROFIT"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.zipCode").value("33101"))
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.sort").value("HIGHEST_PROFIT"))
				.andExpect(jsonPath("$.properties[0].address").value("123 Main St, Miami, FL 33101"))
				.andExpect(jsonPath("$.properties[0].estimatedProfit").value(68500))
				.andExpect(jsonPath("$.properties[0].flipScore").value(82))
				.andExpect(jsonPath("$.properties[0].recommendation").value("Strong Flip Candidate"));
	}
}
