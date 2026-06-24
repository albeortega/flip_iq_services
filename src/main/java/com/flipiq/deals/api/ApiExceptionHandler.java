package com.flipiq.deals.api;

import java.util.List;

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
		return new ApiErrorResponse("Address search provider returned an error.", List.of(exception.getStatusText()));
	}
}
