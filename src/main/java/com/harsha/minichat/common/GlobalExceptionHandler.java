package com.harsha.minichat.common;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApi(ApiException ex) {
		return build(ex.getStatus(), ex.getErrorId(), ex.getMessage(), "");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		String details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.sorted()
				.collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, "minichat.request.invalid", "Invalid request body.", details);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
		return build(HttpStatus.BAD_REQUEST, "minichat.request.unreadable", "Request body is missing or is not valid JSON.", "");
	}

	/** Two requests creating the same username, team or channel at once: the database's unique rule stops the second. */
	@ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleConflict(org.springframework.dao.DataIntegrityViolationException ex) {
		return build(HttpStatus.BAD_REQUEST, "minichat.request.conflict", "That name is already taken.", "");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex) throws Exception {
		// Let Spring answer its own errors (404 for unknown paths, 405, ...) with its normal status.
		if (ex instanceof org.springframework.web.ErrorResponse) {
			throw ex;
		}
		log.error("Unexpected error", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "minichat.server.error", "Something went wrong on the server.", "");
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String id, String message, String details) {
		ApiError body = new ApiError(id, message, details, Ids.newId(), status.value());
		return ResponseEntity.status(status).body(body);
	}
}
