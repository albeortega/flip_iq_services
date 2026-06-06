package com.flipiq.deals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.flipiq.deals.api.DealEvaluationController;
import com.flipiq.deals.model.DealEvaluationResponse;
import com.flipiq.deals.service.DealEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DealEvaluationController.class)
class DealEvaluationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DealEvaluationService dealEvaluationService;

	@Test
	void returnsEvaluationForValidRequest() throws Exception {
		when(dealEvaluationService.evaluate(any())).thenReturn(new DealEvaluationResponse(
				"123 Main Street",
				new BigDecimal("90000.00"),
				new BigDecimal("250000.00"),
				new BigDecimal("0.70"),
				new BigDecimal("175000.00"),
				new BigDecimal("35000.00"),
				new BigDecimal("15000.00"),
				new BigDecimal("15000.00"),
				new BigDecimal("7000.00"),
				new BigDecimal("25000.00"),
				new BigDecimal("162000.00"),
				new BigDecimal("93000.00"),
				new BigDecimal("88000.00"),
				new BigDecimal("3000.00"),
				"REVIEW"));

		mockMvc.perform(post("/api/deals/evaluate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "propertyAddress": "123 Main Street",
								  "purchasePrice": 90000,
								  "afterRepairValue": 250000,
								  "rehabCosts": 35000,
								  "financingCosts": 15000,
								  "holdingCosts": 15000,
								  "sellingCosts": 7000,
								  "profitBuffer": 25000
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.maximumOffer").value(93000.00))
				.andExpect(jsonPath("$.projectedProfit").value(88000.00))
				.andExpect(jsonPath("$.recommendation").value("REVIEW"));
	}

	@Test
	void allowsCorsPreflightForEvaluationEndpoint() throws Exception {
		mockMvc.perform(options("/api/deals/evaluate")
						.header("Origin", "https://flip-iq.example")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "https://flip-iq.example"))
				.andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")))
				.andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("content-type")));
	}

	@Test
	void rejectsMissingNumbers() throws Exception {
		mockMvc.perform(post("/api/deals/evaluate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Deal evaluation request is invalid."))
				.andExpect(jsonPath("$.errors").isArray());
	}
}
