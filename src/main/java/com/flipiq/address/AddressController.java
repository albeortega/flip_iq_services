package com.flipiq.address;

import java.util.List;

import com.flipiq.address.dto.AddressDetails;
import com.flipiq.address.dto.AddressSuggestion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

	private final GooglePlacesService googlePlacesService;

	public AddressController(GooglePlacesService googlePlacesService) {
		this.googlePlacesService = googlePlacesService;
	}

	@GetMapping("/autocomplete")
	public List<AddressSuggestion> autocomplete(
			@RequestParam String input,
			@RequestParam String sessionToken) {
		return googlePlacesService.autocomplete(input, sessionToken);
	}

	@GetMapping("/details")
	public AddressDetails details(
			@RequestParam String placeId,
			@RequestParam String sessionToken) {
		return googlePlacesService.getPlaceDetails(placeId, sessionToken);
	}
}
