package com.flipiq.property.dto;

import java.math.BigDecimal;
import java.util.List;

public record FlipOpportunityResponse(
		String zipCode,
		Integer count,
		String sort,
		Filters filters,
		List<FlipOpportunityPropertyDto> properties) {

	public record Filters(
			BigDecimal minProfit,
			BigDecimal minRoi,
			BigDecimal minDiscount) {
	}
}
