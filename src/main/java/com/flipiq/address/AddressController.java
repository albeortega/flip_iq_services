package com.flipiq.address;

import java.util.List;

import com.flipiq.address.dto.AddressDetails;
import com.flipiq.address.dto.AddressSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

	private static final Logger log = LoggerFactory.getLogger(AddressController.class);

	private final GooglePlacesService googlePlacesService;

	public AddressController(GooglePlacesService googlePlacesService) {
		this.googlePlacesService = googlePlacesService;
	}

	@GetMapping("/autocomplete")
	public List<AddressSuggestion> autocomplete(
			@RequestParam String input,
			@RequestParam String sessionToken) {
		log.info(
				"Address autocomplete API entered: input='{}', inputLength={}, sessionToken={}",
				input,
				input == null ? 0 : input.length(),
				sessionToken);
		List<AddressSuggestion> suggestions = googlePlacesService.autocomplete(input, sessionToken);
		log.info(
				"Address autocomplete API completed: input='{}', suggestionCount={}, sessionToken={}",
				input,
				suggestions.size(),
				sessionToken);
		return suggestions;
	}

	@GetMapping("/details")
	public AddressDetails details(
			@RequestParam String placeId,
			@RequestParam String sessionToken) {
		log.info("Address details API entered: placeId={}, sessionToken={}", placeId, sessionToken);
		AddressDetails details = googlePlacesService.getPlaceDetails(placeId, sessionToken);
		log.info(
				"Address details API completed: placeId={}, formattedAddress='{}', sessionToken={}",
				placeId,
				details.formattedAddress(),
				sessionToken);
		return details;
	}
}
