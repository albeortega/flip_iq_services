package com.flipiq.property;

import com.flipiq.property.dto.EnrichedPropertyResponse;
import com.flipiq.property.dto.PropertyEnrichmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertyEnrichmentController {

	private static final Logger log = LoggerFactory.getLogger(PropertyEnrichmentController.class);

	private final PropertyEnrichmentService propertyEnrichmentService;

	public PropertyEnrichmentController(PropertyEnrichmentService propertyEnrichmentService) {
		this.propertyEnrichmentService = propertyEnrichmentService;
	}

	@PostMapping("/enrich")
	public EnrichedPropertyResponse enrich(@RequestBody PropertyEnrichmentRequest request) {
		log.info(
				"Property enrich API entered: address='{}', placeId={}, sessionTokenPresent={}",
				request.address(),
				request.placeId(),
				request.sessionToken() != null && !request.sessionToken().isBlank());
		EnrichedPropertyResponse response = propertyEnrichmentService.enrich(request);
		log.info(
				"Property enrich API completed: address='{}', estimatedValue={}, lastSalePrice={}, livingArea={}",
				response.address(),
				response.estimatedValue(),
				response.lastSalePrice(),
				response.livingArea());
		return response;
	}
}
