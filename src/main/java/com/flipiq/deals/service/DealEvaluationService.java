package com.flipiq.deals.service;

import com.flipiq.deals.model.DealEvaluationRequest;
import com.flipiq.deals.model.DealEvaluationResponse;
import com.flipiq.deals.model.AiDealReviewResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DealEvaluationService {

	private static final BigDecimal DEFAULT_MAO_RULE_PERCENTAGE = new BigDecimal("0.70");
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final int MONEY_SCALE = 2;
	private static final int PERCENT_SCALE = 2;

	public DealEvaluationResponse evaluate(DealEvaluationRequest request) {
		BigDecimal purchasePrice = money(request.purchasePrice());
		BigDecimal afterRepairValue = money(request.afterRepairValue());
		BigDecimal maoRulePercentage = request.maoRulePercentage() == null
				? DEFAULT_MAO_RULE_PERCENTAGE
				: request.maoRulePercentage();
		BigDecimal rehabCosts = totalRepairCost(request);
		BigDecimal holdingCosts = moneyOrZero(request.holdingCosts());
		BigDecimal sellingCosts = moneyOrZero(request.sellingCosts());
		BigDecimal closingCosts = totalClosingCosts(request, afterRepairValue);
		BigDecimal profitBuffer = moneyOrZero(request.profitBuffer());
		Integer holdingMonths = request.holdingMonths() == null ? 0 : Math.max(request.holdingMonths(), 0);
		DealEvaluationResponse.FinancingBreakdown financing = financingBreakdown(
				request.financing(),
				purchasePrice,
				holdingMonths,
				request.financingCosts());
		BigDecimal financingCosts = financing.totalFinancingCost();
		BigDecimal ruleValue = money(afterRepairValue.multiply(maoRulePercentage));
		BigDecimal maximumAllowableOffer = money(ruleValue.subtract(rehabCosts));
		BigDecimal maximumOffer = money(maximumAllowableOffer
				.subtract(holdingCosts)
				.subtract(sellingCosts)
				.subtract(closingCosts)
				.subtract(profitBuffer));
		BigDecimal totalProjectCost = money(purchasePrice
				.add(rehabCosts)
				.add(financingCosts)
				.add(holdingCosts)
				.add(sellingCosts)
				.add(closingCosts));
		BigDecimal projectedProfit = money(afterRepairValue.subtract(totalProjectCost));
		BigDecimal roi = percent(projectedProfit, totalProjectCost);
		BigDecimal profitMargin = percent(projectedProfit, afterRepairValue);
		BigDecimal offerSpread = money(maximumOffer.subtract(purchasePrice));
		boolean isOfferAcceptable = purchasePrice.compareTo(maximumAllowableOffer) <= 0;
		List<String> riskReasons = riskReasons(
				profitMargin,
				percent(rehabCosts, afterRepairValue),
				holdingMonths,
				financingCosts,
				!isOfferAcceptable,
				request.marketRisk());
		String riskLevel = riskLevel(riskReasons);
		int dealScore = dealScore(roi, profitMargin, isOfferAcceptable, percent(rehabCosts, afterRepairValue),
				holdingMonths, financingCosts, closingCosts, request.marketRisk());
		String scoreGrade = scoreGrade(dealScore);

		return new DealEvaluationResponse(
				request.propertyAddress(),
				purchasePrice,
				afterRepairValue,
				afterRepairValue,
				maoRulePercentage,
				maoRulePercentage,
				ruleValue,
				rehabCosts,
				rehabCosts,
				rehabCosts,
				financingCosts,
				holdingCosts,
				sellingCosts,
				closingCosts,
				profitBuffer,
				totalProjectCost,
				maximumOffer,
				maximumAllowableOffer,
				isOfferAcceptable,
				projectedProfit,
				roi,
				profitMargin,
				offerSpread,
				dealScore,
				scoreGrade,
				riskLevel,
				riskReasons,
				recommendation(dealScore),
				financing,
				rentalAnalysis(request.rentalAnalysis(), afterRepairValue, financing.monthlyPayment(), financing.loanAmount()),
				compsAnalysis(request.subjectPropertySquareFeet(), request.comparableSales()),
				sensitivityScenarios(purchasePrice, afterRepairValue, rehabCosts, financingCosts, holdingCosts, sellingCosts,
						closingCosts));
	}

	public AiDealReviewResponse analyzeAi(DealEvaluationRequest request) {
		DealEvaluationResponse evaluation = evaluate(request);
		List<String> strengths = new ArrayList<>();
		List<String> warnings = new ArrayList<>(evaluation.riskReasons());

		if (evaluation.projectedProfit().signum() > 0) {
			strengths.add("Positive projected profit");
		}
		if (evaluation.isOfferAcceptable()) {
			strengths.add("Purchase price is within MAO");
		}
		if (evaluation.dealScore() >= 80) {
			strengths.add("Deal score is in buy range");
		}
		if (!evaluation.isOfferAcceptable() && !warnings.contains("Purchase price is above MAO")) {
			warnings.add("Purchase price is above MAO");
		}

		String summary = "This deal has a projected profit of $" + evaluation.projectedProfit()
				+ " and an ROI of " + evaluation.roi() + "%. The risk level is " + evaluation.riskLevel()
				+ " with a deal score of " + evaluation.dealScore() + ".";
		return new AiDealReviewResponse(summary, strengths, warnings, evaluation.recommendation());
	}

	private BigDecimal totalRepairCost(DealEvaluationRequest request) {
		if (request.rehabItems() == null || request.rehabItems().isEmpty()) {
			return moneyOrZero(request.rehabCosts());
		}
		return money(request.rehabItems().stream()
				.map(DealEvaluationRequest.RehabItem::estimatedCost)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
	}

	private DealEvaluationResponse.FinancingBreakdown financingBreakdown(
			DealEvaluationRequest.FinancingInput financing,
			BigDecimal purchasePrice,
			int holdingMonths,
			BigDecimal fallbackFinancingCosts) {
		if (financing == null || isCash(financing.financingType())) {
			return new DealEvaluationResponse.FinancingBreakdown(
					financing == null ? "Cash" : financing.financingType(),
					BigDecimal.ZERO.setScale(PERCENT_SCALE),
					BigDecimal.ZERO.setScale(MONEY_SCALE),
					BigDecimal.ZERO.setScale(PERCENT_SCALE),
					0,
					BigDecimal.ZERO.setScale(PERCENT_SCALE),
					BigDecimal.ZERO.setScale(MONEY_SCALE),
					BigDecimal.ZERO.setScale(MONEY_SCALE),
					BigDecimal.ZERO.setScale(MONEY_SCALE),
					moneyOrZero(fallbackFinancingCosts));
		}

		BigDecimal downPaymentPercentage = percentValue(financing.downPaymentPercentage());
		BigDecimal interestRate = percentValue(financing.interestRate());
		BigDecimal points = percentValue(financing.points());
		int loanTermMonths = financing.loanTermMonths() == null || financing.loanTermMonths() <= 0
				? 360
				: financing.loanTermMonths();
		BigDecimal loanAmount = money(purchasePrice.subtract(purchasePrice.multiply(downPaymentPercentage)
				.divide(ONE_HUNDRED, MONEY_SCALE + 4, RoundingMode.HALF_UP)));
		BigDecimal monthlyPayment = monthlyPayment(loanAmount, interestRate, loanTermMonths);
		BigDecimal totalInterestCost = money(loanAmount
				.multiply(interestRate)
				.divide(ONE_HUNDRED, MONEY_SCALE + 4, RoundingMode.HALF_UP)
				.divide(new BigDecimal("12"), MONEY_SCALE + 4, RoundingMode.HALF_UP)
				.multiply(new BigDecimal(holdingMonths)));
		BigDecimal pointsCost = money(loanAmount.multiply(points).divide(ONE_HUNDRED, MONEY_SCALE + 4, RoundingMode.HALF_UP));
		BigDecimal totalFinancingCost = money(totalInterestCost.add(pointsCost).add(moneyOrZero(fallbackFinancingCosts)));

		return new DealEvaluationResponse.FinancingBreakdown(
				financing.financingType(),
				downPaymentPercentage,
				loanAmount,
				interestRate,
				loanTermMonths,
				points,
				monthlyPayment,
				totalInterestCost,
				pointsCost,
				totalFinancingCost);
	}

	private DealEvaluationResponse.RentalAnalysis rentalAnalysis(
			DealEvaluationRequest.RentalInput rental,
			BigDecimal propertyValue,
			BigDecimal calculatedMonthlyMortgage,
			BigDecimal annualDebtBase) {
		if (rental == null) {
			return null;
		}
		BigDecimal monthlyRent = moneyOrZero(rental.monthlyRent());
		BigDecimal monthlyMortgage = rental.monthlyMortgage() == null
				? calculatedMonthlyMortgage
				: money(rental.monthlyMortgage());
		BigDecimal vacancy = monthlyRent.multiply(percentValue(rental.vacancyRate()))
				.divide(ONE_HUNDRED, MONEY_SCALE + 4, RoundingMode.HALF_UP);
		BigDecimal management = monthlyRent.multiply(percentValue(rental.propertyManagementFee()))
				.divide(ONE_HUNDRED, MONEY_SCALE + 4, RoundingMode.HALF_UP);
		BigDecimal operatingExpenses = money(moneyOrZero(rental.propertyTax())
				.add(moneyOrZero(rental.insurance()))
				.add(moneyOrZero(rental.hoa()))
				.add(moneyOrZero(rental.maintenance()))
				.add(vacancy)
				.add(management));
		BigDecimal monthlyExpenses = money(operatingExpenses.add(monthlyMortgage));
		BigDecimal monthlyCashFlow = money(monthlyRent.subtract(monthlyExpenses));
		BigDecimal annualCashFlow = money(monthlyCashFlow.multiply(new BigDecimal("12")));
		BigDecimal annualNoi = money(monthlyRent.subtract(operatingExpenses).multiply(new BigDecimal("12")));
		BigDecimal annualDebtService = money(monthlyMortgage.multiply(new BigDecimal("12")));
		BigDecimal cashInvested = rental.cashInvested() == null ? annualDebtBase : money(rental.cashInvested());

		return new DealEvaluationResponse.RentalAnalysis(
				monthlyRent,
				monthlyMortgage,
				monthlyExpenses,
				monthlyCashFlow,
				annualCashFlow,
				percent(annualNoi, propertyValue),
				percent(annualCashFlow, cashInvested),
				ratio(annualNoi, annualDebtService));
	}

	private DealEvaluationResponse.CompsAnalysis compsAnalysis(
			BigDecimal subjectPropertySquareFeet,
			List<DealEvaluationRequest.ComparableSaleInput> comparableSales) {
		if (subjectPropertySquareFeet == null || comparableSales == null || comparableSales.isEmpty()) {
			return null;
		}
		List<DealEvaluationResponse.ComparableSale> comps = comparableSales.stream()
				.map(comp -> new DealEvaluationResponse.ComparableSale(
						comp.address(),
						money(comp.salePrice()),
						money(comp.squareFeet()),
						money(comp.salePrice().divide(comp.squareFeet(), MONEY_SCALE + 4, RoundingMode.HALF_UP)),
						comp.distanceMiles(),
						comp.soldDate(),
						comp.bedrooms(),
						comp.bathrooms()))
				.toList();
		BigDecimal averagePricePerSqFt = money(comps.stream()
				.map(DealEvaluationResponse.ComparableSale::pricePerSqFt)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.divide(new BigDecimal(comps.size()), MONEY_SCALE + 4, RoundingMode.HALF_UP));
		BigDecimal estimatedArv = money(subjectPropertySquareFeet.multiply(averagePricePerSqFt));

		return new DealEvaluationResponse.CompsAnalysis(averagePricePerSqFt, estimatedArv, comps);
	}

	private List<DealEvaluationResponse.SensitivityScenario> sensitivityScenarios(
			BigDecimal purchasePrice,
			BigDecimal afterRepairValue,
			BigDecimal rehabCosts,
			BigDecimal financingCosts,
			BigDecimal holdingCosts,
			BigDecimal sellingCosts,
			BigDecimal closingCosts) {
		return List.of(
				sensitivityScenario("Best Case", purchasePrice, afterRepairValue.multiply(new BigDecimal("1.05")),
						rehabCosts.multiply(new BigDecimal("0.90")), financingCosts, holdingCosts.multiply(new BigDecimal("0.90")),
						sellingCosts, closingCosts),
				sensitivityScenario("Expected Case", purchasePrice, afterRepairValue, rehabCosts, financingCosts, holdingCosts,
						sellingCosts, closingCosts),
				sensitivityScenario("Worst Case", purchasePrice, afterRepairValue.multiply(new BigDecimal("0.95")),
						rehabCosts.multiply(new BigDecimal("1.15")), financingCosts, holdingCosts.multiply(new BigDecimal("1.20")),
						sellingCosts, closingCosts));
	}

	private DealEvaluationResponse.SensitivityScenario sensitivityScenario(
			String name,
			BigDecimal purchasePrice,
			BigDecimal afterRepairValue,
			BigDecimal rehabCosts,
			BigDecimal financingCosts,
			BigDecimal holdingCosts,
			BigDecimal sellingCosts,
			BigDecimal closingCosts) {
		BigDecimal cost = money(purchasePrice.add(rehabCosts).add(financingCosts).add(holdingCosts).add(sellingCosts)
				.add(closingCosts));
		BigDecimal profit = money(afterRepairValue.subtract(cost));
		return new DealEvaluationResponse.SensitivityScenario(name, profit, percent(profit, cost));
	}

	private BigDecimal totalClosingCosts(DealEvaluationRequest request, BigDecimal afterRepairValue) {
		BigDecimal total = moneyOrZero(request.closingCosts());
		if (request.floridaCosts() == null) {
			return total;
		}
		DealEvaluationRequest.FloridaCosts costs = request.floridaCosts();
		BigDecimal commission = afterRepairValue.multiply(percentValue(costs.realtorCommissionPercentage()))
				.divide(ONE_HUNDRED, MONEY_SCALE + 4, RoundingMode.HALF_UP);
		return money(total
				.add(moneyOrZero(costs.propertyTaxes()))
				.add(moneyOrZero(costs.insurance()))
				.add(moneyOrZero(costs.hoa()))
				.add(commission)
				.add(moneyOrZero(costs.titleFees()))
				.add(moneyOrZero(costs.transferTaxes()))
				.add(moneyOrZero(costs.permitCosts()))
				.add(moneyOrZero(costs.floodInsurance())));
	}

	private List<String> riskReasons(
			BigDecimal profitMargin,
			BigDecimal rehabCostPercentage,
			int holdingMonths,
			BigDecimal financingCost,
			boolean purchasePriceAboveMao,
			String marketRisk) {
		List<String> reasons = new ArrayList<>();
		if (profitMargin.compareTo(new BigDecimal("15.00")) < 0) {
			reasons.add("Profit margin is below 15%");
		}
		if (rehabCostPercentage.compareTo(new BigDecimal("20.00")) > 0) {
			reasons.add("Rehab cost is above 20% of ARV");
		}
		if (holdingMonths > 6) {
			reasons.add("Holding period is longer than 6 months");
		}
		if (financingCost.signum() > 0) {
			reasons.add("Financing costs reduce projected returns");
		}
		if (purchasePriceAboveMao) {
			reasons.add("Purchase price is above MAO");
		}
		if ("high".equalsIgnoreCase(marketRisk)) {
			reasons.add("Market risk is high");
		}
		return reasons;
	}

	private String riskLevel(List<String> riskReasons) {
		if (riskReasons.size() >= 4) {
			return "High";
		}
		if (riskReasons.size() >= 2) {
			return "Medium";
		}
		return "Low";
	}

	private int dealScore(
			BigDecimal roi,
			BigDecimal profitMargin,
			boolean isOfferAcceptable,
			BigDecimal rehabCostPercentage,
			int holdingMonths,
			BigDecimal financingCost,
			BigDecimal closingCost,
			String marketRisk) {
		int score = 100;
		score -= penaltyBelow(roi, new BigDecimal("20.00"), 20);
		score -= penaltyBelow(profitMargin, new BigDecimal("15.00"), 20);
		if (!isOfferAcceptable) {
			score -= 15;
		}
		if (rehabCostPercentage.compareTo(new BigDecimal("20.00")) > 0) {
			score -= 10;
		}
		if (holdingMonths > 6) {
			score -= 10;
		}
		if (financingCost.signum() > 0) {
			score -= 5;
		}
		if (closingCost.signum() > 0) {
			score -= 5;
		}
		if ("high".equalsIgnoreCase(marketRisk)) {
			score -= 10;
		}
		else if ("medium".equalsIgnoreCase(marketRisk)) {
			score -= 5;
		}
		return Math.max(0, Math.min(100, score));
	}

	private int penaltyBelow(BigDecimal value, BigDecimal target, int maxPenalty) {
		if (value.compareTo(target) >= 0) {
			return 0;
		}
		BigDecimal shortfall = target.subtract(value).max(BigDecimal.ZERO);
		return Math.min(maxPenalty, shortfall.setScale(0, RoundingMode.UP).intValue());
	}

	private String scoreGrade(int dealScore) {
		if (dealScore >= 90) {
			return "A";
		}
		if (dealScore >= 80) {
			return "B";
		}
		if (dealScore >= 70) {
			return "C";
		}
		if (dealScore >= 60) {
			return "D";
		}
		return "F";
	}

	private String recommendation(int dealScore) {
		if (dealScore >= 90) {
			return "Strong Buy";
		}
		if (dealScore >= 80) {
			return "Good Deal";
		}
		if (dealScore >= 70) {
			return "Review Carefully";
		}
		if (dealScore >= 60) {
			return "High Risk";
		}
		return "Avoid";
	}

	private BigDecimal monthlyPayment(BigDecimal loanAmount, BigDecimal annualRate, int loanTermMonths) {
		if (loanAmount.signum() == 0 || annualRate.signum() == 0) {
			return money(loanAmount.divide(new BigDecimal(loanTermMonths), MONEY_SCALE + 4, RoundingMode.HALF_UP));
		}
		double principal = loanAmount.doubleValue();
		double monthlyRate = annualRate.divide(ONE_HUNDRED, MONEY_SCALE + 6, RoundingMode.HALF_UP)
				.divide(new BigDecimal("12"), MONEY_SCALE + 6, RoundingMode.HALF_UP)
				.doubleValue();
		double payment = principal * (monthlyRate * Math.pow(1 + monthlyRate, loanTermMonths))
				/ (Math.pow(1 + monthlyRate, loanTermMonths) - 1);
		return money(BigDecimal.valueOf(payment));
	}

	private boolean isCash(String financingType) {
		return financingType == null || "cash".equalsIgnoreCase(financingType);
	}

	private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
		if (denominator == null || denominator.signum() == 0) {
			return BigDecimal.ZERO.setScale(PERCENT_SCALE);
		}
		return numerator.multiply(ONE_HUNDRED).divide(denominator, PERCENT_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
		if (denominator == null || denominator.signum() == 0) {
			return BigDecimal.ZERO.setScale(PERCENT_SCALE);
		}
		return numerator.divide(denominator, PERCENT_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal percentValue(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(PERCENT_SCALE) : value.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal moneyOrZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE) : money(value);
	}

	private BigDecimal money(BigDecimal value) {
		return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}
}
