package com.harsha.minichat.common;

/**
 * Error body. REA recorded these five fields on the 403 and 404 responses.
 */
public record ApiError(String id, String message, String detailedError, String requestId, int statusCode) {
}
