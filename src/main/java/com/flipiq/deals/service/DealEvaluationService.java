package com.flipiq.deals.service;

import java.math.BigDecimal;

import com.flipiq.deals.model.DealEvaluationRequest;
import com.flipiq.deals.model.DealEvaluationResponse;
import org.springframework.stereotype.Service;

@Service
public class DealEvaluationService {

	private static final BigDecimal OFFER_RULE_PERCENTAGE = new BigDecimal("0.70");
	private static final int MONEY_SCALE = 2;

	public DealEvaluationResponse evaluate(DealEvaluationRequest request) {
		BigDecimal purchasePrice = money(request.purchasePrice());
		BigDecimal afterRepairValue = money(request.afterRepairValue());
		BigDecimal rehabCosts = money(request.rehabCosts());
		BigDecimal financingCosts = money(request.financingCosts());
		BigDecimal holdingCosts = money(request.holdingCosts());
		BigDecimal sellingCosts = money(request.sellingCosts());
		BigDecimal profitBuffer = money(request.profitBuffer());
		BigDecimal ruleValue = money(afterRepairValue.multiply(OFFER_RULE_PERCENTAGE));
		BigDecimal maximumOffer = money(ruleValue
				.subtract(rehabCosts)
				.subtract(holdingCosts)
				.subtract(sellingCosts)
				.subtract(profitBuffer));
		BigDecimal totalProjectCost = money(purchasePrice
				.add(rehabCosts)
				.add(financingCosts)
				.add(holdingCosts)
				.add(sellingCosts));
		BigDecimal projectedProfit = money(afterRepairValue.subtract(totalProjectCost));
		BigDecimal offerSpread = money(maximumOffer.subtract(purchasePrice));

		return new DealEvaluationResponse(
				request.propertyAddress(),
				purchasePrice,
				afterRepairValue,
				OFFER_RULE_PERCENTAGE,
				ruleValue,
				rehabCosts,
				financingCosts,
				holdingCosts,
				sellingCosts,
				profitBuffer,
				totalProjectCost,
				maximumOffer,
				projectedProfit,
				offerSpread,
				recommendation(maximumOffer, purchasePrice, projectedProfit));
	}

	private String recommendation(BigDecimal maximumOffer, BigDecimal purchasePrice, BigDecimal projectedProfit) {
		if (maximumOffer.signum() <= 0 || purchasePrice.compareTo(maximumOffer) > 0 || projectedProfit.signum() <= 0) {
			return "PASS";
		}
		return "REVIEW";
	}

	private BigDecimal money(BigDecimal value) {
		return value.setScale(MONEY_SCALE);
	}
}
