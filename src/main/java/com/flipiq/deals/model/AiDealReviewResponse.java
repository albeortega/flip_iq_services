package com.flipiq.deals.model;

import java.util.List;

public record AiDealReviewResponse(
		String summary,
		List<String> strengths,
		List<String> warnings,
		String recommendation) {
}
