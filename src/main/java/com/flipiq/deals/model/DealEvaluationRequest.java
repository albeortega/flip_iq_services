package com.flipiq.deals.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DealEvaluationRequest(
		String propertyAddress,
		@NotNull @DecimalMin(value = "0.00") BigDecimal purchasePrice,
		@NotNull @DecimalMin(value = "0.01") BigDecimal afterRepairValue,
		@NotNull @DecimalMin(value = "0.00") BigDecimal rehabCosts,
		@NotNull @DecimalMin(value = "0.00") BigDecimal financingCosts,
		@NotNull @DecimalMin(value = "0.00") BigDecimal holdingCosts,
		@NotNull @DecimalMin(value = "0.00") BigDecimal sellingCosts,
		@NotNull @DecimalMin(value = "0.00") BigDecimal profitBuffer) {
}
