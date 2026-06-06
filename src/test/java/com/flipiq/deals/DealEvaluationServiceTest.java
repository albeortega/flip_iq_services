package com.flipiq.deals;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.flipiq.deals.model.DealEvaluationRequest;
import com.flipiq.deals.model.DealEvaluationResponse;
import com.flipiq.deals.service.DealEvaluationService;
import org.junit.jupiter.api.Test;

class DealEvaluationServiceTest {

	private final DealEvaluationService service = new DealEvaluationService();

	@Test
	void calculatesMaximumOfferUsingSeventyPercentRule() {
		DealEvaluationResponse response = service.evaluate(new DealEvaluationRequest(
				"123 Main Street",
				new BigDecimal("90000"),
				new BigDecimal("250000"),
				new BigDecimal("35000"),
				new BigDecimal("8000"),
				new BigDecimal("15000"),
				new BigDecimal("7000"),
				new BigDecimal("25000")));

		assertThat(response.ruleValue()).isEqualByComparingTo("175000.00");
		assertThat(response.maximumOffer()).isEqualByComparingTo("93000.00");
		assertThat(response.totalProjectCost()).isEqualByComparingTo("155000.00");
		assertThat(response.projectedProfit()).isEqualByComparingTo("95000.00");
		assertThat(response.offerSpread()).isEqualByComparingTo("3000.00");
		assertThat(response.recommendation()).isEqualTo("REVIEW");
	}
}
