package com.flipiq.property.dto;

import java.math.BigDecimal;
import java.util.List;

public record FlipOpportunityPropertyDto(
		String id,
		String address,
		String city,
		String state,
		String zipCode,
		String propertyType,
		BigDecimal listPrice,
		BigDecimal estimatedValue,
		BigDecimal estimatedRehabCost,
		BigDecimal closingCosts,
		BigDecimal holdingCosts,
		BigDecimal estimatedProfit,
		BigDecimal roiPercent,
		BigDecimal discountPercent,
		BigDecimal priceDropAmount,
		Integer daysOnMarket,
		String rehabRisk,
		Integer bedrooms,
		BigDecimal bathrooms,
		Integer livingArea,
		Integer yearBuilt,
		Double latitude,
		Double longitude,
		Integer flipScore,
		String recommendation,
		List<String> highlights) {
}
