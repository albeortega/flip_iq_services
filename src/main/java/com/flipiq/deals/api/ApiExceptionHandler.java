package com.flipiq.deals.api;

import java.util.List;

import com.flipiq.address.AddressSearchConfigurationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class ApiExceptionHandler {

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
		return new ApiErrorResponse(exception.getMessage(), List.of());
	}

	@ExceptionHandler(RestClientResponseException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public ApiErrorResponse handleUpstreamGoogleError(RestClientResponseException exception) {
		String responseBody = exception.getResponseBodyAsString();
		List<String> errors = responseBody == null || responseBody.isBlank()
				? List.of(exception.getStatusText())
				: List.of(exception.getStatusText(), truncate(responseBody));
		return new ApiErrorResponse("Address search provider returned an error.", errors);
	}

	@ExceptionHandler(AddressSearchConfigurationException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public ApiErrorResponse handleAddressSearchConfiguration(AddressSearchConfigurationException exception) {
		return new ApiErrorResponse(exception.getMessage(), List.of());
	}

	private String truncate(String value) {
		return value.length() <= 1000 ? value : value.substring(0, 1000);
	}
}
