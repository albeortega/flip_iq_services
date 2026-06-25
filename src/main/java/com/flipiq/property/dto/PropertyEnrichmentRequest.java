package com.flipiq.property.dto;

public record PropertyEnrichmentRequest(
		String address,
		String placeId,
		String sessionToken) {
}
