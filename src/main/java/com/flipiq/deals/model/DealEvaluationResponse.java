package com.flipiq.deals.model;

import java.math.BigDecimal;

public record DealEvaluationResponse(
		String propertyAddress,
		BigDecimal purchasePrice,
		BigDecimal afterRepairValue,
		BigDecimal offerRulePercentage,
		BigDecimal ruleValue,
		BigDecimal rehabCosts,
		BigDecimal financingCosts,
		BigDecimal holdingCosts,
		BigDecimal sellingCosts,
		BigDecimal profitBuffer,
		BigDecimal totalProjectCost,
		BigDecimal maximumOffer,
		BigDecimal projectedProfit,
		BigDecimal offerSpread,
		String recommendation) {
}
