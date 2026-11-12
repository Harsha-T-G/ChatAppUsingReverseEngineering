package com.harsha.minichat.common;

import org.springframework.http.HttpStatus;

/**
 * Business error that becomes the recorded error body: id, message, detailed_error, request_id, status_code.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String errorId;

	public ApiException(HttpStatus status, String errorId, String message) {
		super(message);
		this.status = status;
		this.errorId = errorId;
	}

	public static ApiException badRequest(String errorId, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, errorId, message);
	}

	public static ApiException unauthorized(String errorId, String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, errorId, message);
	}

	public static ApiException forbidden(String errorId, String message) {
		return new ApiException(HttpStatus.FORBIDDEN, errorId, message);
	}

	public static ApiException notFound(String errorId, String message) {
		return new ApiException(HttpStatus.NOT_FOUND, errorId, message);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getErrorId() {
		return errorId;
	}
}
