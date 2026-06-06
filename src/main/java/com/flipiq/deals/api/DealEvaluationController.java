package com.flipiq.deals.api;

import com.flipiq.deals.model.DealEvaluationRequest;
import com.flipiq.deals.model.DealEvaluationResponse;
import com.flipiq.deals.service.DealEvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deals")
public class DealEvaluationController {

	private final DealEvaluationService dealEvaluationService;

	public DealEvaluationController(DealEvaluationService dealEvaluationService) {
		this.dealEvaluationService = dealEvaluationService;
	}

	@PostMapping("/evaluate")
	@ResponseStatus(HttpStatus.OK)
	public DealEvaluationResponse evaluate(@Valid @RequestBody DealEvaluationRequest request) {
		return dealEvaluationService.evaluate(request);
	}
}
