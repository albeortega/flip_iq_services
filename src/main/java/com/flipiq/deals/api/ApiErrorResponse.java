package com.flipiq.deals.api;

import java.util.List;

public record ApiErrorResponse(
		String message,
		List<String> errors) {
}
