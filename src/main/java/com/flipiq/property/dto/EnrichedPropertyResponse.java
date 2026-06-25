package com.flipiq.property.dto;

import java.math.BigDecimal;

public record EnrichedPropertyResponse(
		String address,
		BigDecimal zestimate,
		BigDecimal estimatedValue,
		BigDecimal lastSalePrice,
		String lastSaleDate,
		Integer bedrooms,
		BigDecimal bathrooms,
		Integer livingArea,
		String placeId,
		String formattedAddress,
		String streetNumber,
		String route,
		String city,
		String county,
		String state,
		String stateCode,
		String zipCode,
		String country,
		Double latitude,
		Double longitude) {
}
