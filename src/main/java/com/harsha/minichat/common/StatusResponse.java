package com.harsha.minichat.common;

/** Recorded response for DELETE /api/v4/posts/{id}: one field, status. Also used by logout. */
public record StatusResponse(String status) {

	public static StatusResponse ok() {
		return new StatusResponse("OK");
	}
}
