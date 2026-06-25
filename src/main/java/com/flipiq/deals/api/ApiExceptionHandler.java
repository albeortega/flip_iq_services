package com.flipiq.deals.api;

import java.util.List;

import com.flipiq.address.AddressSearchConfigurationException;
import com.flipiq.property.PropertyEnrichmentConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleValidation(MethodArgumentNotValidException exception) {
		List<String> errors = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
				.toList();

		return new ApiErrorResponse("Deal evaluation request is invalid.", errors);
	}

	@ExceptionHandler({
			IllegalArgumentException.class,
			MissingServletRequestParameterException.class
	})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleBadRequest(Exception exception) {
		log.warn("Bad API request: {}", exception.getMessage());
		return new ApiErrorResponse(exception.getMessage(), List.of());
	}

	@ExceptionHandler(RestClientResponseException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public ApiErrorResponse handleUpstreamGoogleError(RestClientResponseException exception) {
		String responseBody = exception.getResponseBodyAsString();
		List<String> errors = responseBody == null || responseBody.isBlank()
				? List.of(exception.getStatusText())
				: List.of(exception.getStatusText(), truncate(responseBody));
		log.error(
				"External provider error: statusCode={}, statusText='{}', responseBody='{}'",
				exception.getStatusCode(),
				exception.getStatusText(),
				truncate(responseBody == null ? "" : responseBody));
		return new ApiErrorResponse("External property data provider returned an error.", errors);
	}

	@ExceptionHandler(AddressSearchConfigurationException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public ApiErrorResponse handleAddressSearchConfiguration(AddressSearchConfigurationException exception) {
		log.error("Address search configuration error: {}", exception.getMessage());
		return new ApiErrorResponse(exception.getMessage(), List.of());
	}

	@ExceptionHandler(PropertyEnrichmentConfigurationException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public ApiErrorResponse handlePropertyEnrichmentConfiguration(PropertyEnrichmentConfigurationException exception) {
		log.error("Property enrichment configuration error: {}", exception.getMessage());
		return new ApiErrorResponse(exception.getMessage(), List.of());
	}

	private String truncate(String value) {
		return value.length() <= 1000 ? value : value.substring(0, 1000);
	}
}
