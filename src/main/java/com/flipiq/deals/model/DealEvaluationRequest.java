package com.flipiq.deals.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DealEvaluationRequest(
		String propertyAddress,
		@NotNull @DecimalMin(value = "0.00") BigDecimal purchasePrice,
		@JsonAlias("arv")
		@NotNull @DecimalMin(value = "0.01") BigDecimal afterRepairValue,
		@JsonAlias({ "repairCost", "totalRepairCost" })
		@DecimalMin(value = "0.00") BigDecimal rehabCosts,
		@DecimalMin(value = "0.00") BigDecimal financingCosts,
		@DecimalMin(value = "0.00") BigDecimal holdingCosts,
		@DecimalMin(value = "0.00") BigDecimal sellingCosts,
		@DecimalMin(value = "0.00") BigDecimal profitBuffer,
		@DecimalMin(value = "0.01") BigDecimal maoRulePercentage,
		@DecimalMin(value = "0.00") BigDecimal closingCosts) {

	public DealEvaluationRequest(
			String propertyAddress,
			BigDecimal purchasePrice,
			BigDecimal afterRepairValue,
			BigDecimal rehabCosts,
			BigDecimal financingCosts,
			BigDecimal holdingCosts,
			BigDecimal sellingCosts,
			BigDecimal profitBuffer) {
		this(propertyAddress, purchasePrice, afterRepairValue, rehabCosts, financingCosts, holdingCosts, sellingCosts,
				profitBuffer, null, null);
	}
}
