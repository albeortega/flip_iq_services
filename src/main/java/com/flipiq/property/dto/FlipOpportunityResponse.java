package com.flipiq.property.dto;

import java.util.List;

public record FlipOpportunityResponse(
		String zipCode,
		Integer count,
		String sort,
		List<FlipOpportunityPropertyDto> properties) {
}
