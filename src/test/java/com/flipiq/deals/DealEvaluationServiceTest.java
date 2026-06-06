package com.flipiq.deals;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

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
		assertThat(response.maximumAllowableOffer()).isEqualByComparingTo("140000.00");
		assertThat(response.isOfferAcceptable()).isTrue();
		assertThat(response.dealScore()).isEqualTo(95);
		assertThat(response.scoreGrade()).isEqualTo("A");
		assertThat(response.riskLevel()).isEqualTo("Low");
		assertThat(response.recommendation()).isEqualTo("Strong Buy");
	}

	@Test
	void calculatesInvestorAnalysisFromExpandedInputs() {
		DealEvaluationResponse response = service.evaluate(new DealEvaluationRequest(
				"456 Oak Avenue",
				new BigDecimal("200000"),
				new BigDecimal("350000"),
				null,
				BigDecimal.ZERO,
				new BigDecimal("6000"),
				new BigDecimal("10000"),
				BigDecimal.ZERO,
				new BigDecimal("0.70"),
				new BigDecimal("2000"),
				6,
				"Medium",
				List.of(
						new DealEvaluationRequest.RehabItem("Kitchen", new BigDecimal("15000")),
						new DealEvaluationRequest.RehabItem("Bathrooms", new BigDecimal("8000"))),
				new DealEvaluationRequest.FinancingInput(
						"Hard Money Loan",
						new BigDecimal("20"),
						new BigDecimal("12"),
						360,
						new BigDecimal("2")),
				new DealEvaluationRequest.RentalInput(
						new BigDecimal("2800"),
						null,
						new BigDecimal("350"),
						new BigDecimal("200"),
						BigDecimal.ZERO,
						new BigDecimal("250"),
						new BigDecimal("5"),
						new BigDecimal("8"),
						new BigDecimal("60000")),
				new BigDecimal("1800"),
				List.of(
						new DealEvaluationRequest.ComparableSaleInput(
								"10 First St",
								new BigDecimal("360000"),
								new BigDecimal("1800"),
								new BigDecimal("0.4"),
								null,
								3,
								new BigDecimal("2")),
						new DealEvaluationRequest.ComparableSaleInput(
								"11 Second St",
								new BigDecimal("330000"),
								new BigDecimal("1650"),
								new BigDecimal("0.7"),
								null,
								3,
								new BigDecimal("2"))),
				null));

		assertThat(response.totalRepairCost()).isEqualByComparingTo("23000.00");
		assertThat(response.maximumAllowableOffer()).isEqualByComparingTo("222000.00");
		assertThat(response.maximumOffer()).isEqualByComparingTo("204000.00");
		assertThat(response.financing().loanAmount()).isEqualByComparingTo("160000.00");
		assertThat(response.financing().totalInterestCost()).isEqualByComparingTo("9600.00");
		assertThat(response.financing().pointsCost()).isEqualByComparingTo("3200.00");
		assertThat(response.projectedProfit()).isEqualByComparingTo("96200.00");
		assertThat(response.dealScore()).isEqualTo(85);
		assertThat(response.scoreGrade()).isEqualTo("B");
		assertThat(response.rentalAnalysis()).isNotNull();
		assertThat(response.compsAnalysis().averagePricePerSqFt()).isEqualByComparingTo("200.00");
		assertThat(response.compsAnalysis().estimatedArv()).isEqualByComparingTo("360000.00");
		assertThat(response.scenarios()).hasSize(3);
	}
}
