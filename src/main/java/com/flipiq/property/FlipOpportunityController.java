package com.flipiq.property;

import com.flipiq.property.dto.FlipOpportunityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class FlipOpportunityController {

	private static final Logger log = LoggerFactory.getLogger(FlipOpportunityController.class);

	private final FlipOpportunityService flipOpportunityService;

	public FlipOpportunityController(FlipOpportunityService flipOpportunityService) {
		this.flipOpportunityService = flipOpportunityService;
	}

	@GetMapping("/flip-opportunities")
	public FlipOpportunityResponse search(
			@RequestParam String zipCode,
			@RequestParam(required = false) Integer radius,
			@RequestParam(required = false, defaultValue = "BEST_FLIP_SCORE") FlipOpportunitySort sort) {
		log.info(
				"Flip opportunities API entered: zipCode={}, radius={}, sort={}",
				zipCode,
				radius,
				sort);
		FlipOpportunityResponse response = flipOpportunityService.search(
				zipCode,
				sort);
		log.info(
				"Flip opportunities API completed: zipCode={}, count={}, sort={}",
				response.zipCode(),
				response.count(),
				response.sort());
		return response;
	}
}
