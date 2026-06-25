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
		BigDecimal closingCosts = moneyOrZero(request.closingCosts());
		BigDecimal financingCosts = moneyOrZero(request.financingCosts());
		BigDecimal ruleValue = money(afterRepairValue.multiply(maoRulePercentage));
		BigDecimal maximumAllowableOffer = money(ruleValue.subtract(rehabCosts));
		BigDecimal maximumOffer = money(maximumAllowableOffer
				.subtract(holdingCosts)
				.subtract(sellingCosts)
				.subtract(closingCosts));
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
				!isOfferAcceptable);
		String riskLevel = riskLevel(riskReasons);
		int dealScore = dealScore(roi, profitMargin, isOfferAcceptable, closingCosts);
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
				null,
				null,
				null,
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
		return moneyOrZero(request.rehabCosts());
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

	private List<String> riskReasons(
			BigDecimal profitMargin,
			boolean purchasePriceAboveMao) {
		List<String> reasons = new ArrayList<>();
		if (profitMargin.compareTo(new BigDecimal("15.00")) < 0) {
			reasons.add("Profit margin is below 15%");
		}
		if (purchasePriceAboveMao) {
			reasons.add("Purchase price is above MAO");
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
			BigDecimal closingCost) {
		int score = 100;
		score -= penaltyBelow(roi, new BigDecimal("20.00"), 20);
		score -= penaltyBelow(profitMargin, new BigDecimal("15.00"), 20);
		if (!isOfferAcceptable) {
			score -= 15;
		}
		if (closingCost.signum() > 0) {
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

	private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
		if (denominator == null || denominator.signum() == 0) {
			return BigDecimal.ZERO.setScale(PERCENT_SCALE);
		}
		return numerator.multiply(ONE_HUNDRED).divide(denominator, PERCENT_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal moneyOrZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE) : money(value);
	}

	private BigDecimal money(BigDecimal value) {
		return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}
}
