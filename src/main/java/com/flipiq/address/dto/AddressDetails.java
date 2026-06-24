package com.flipiq.address.dto;

public record AddressDetails(
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
