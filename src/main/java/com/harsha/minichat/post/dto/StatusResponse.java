package com.harsha.minichat.post.dto;

/** Recorded response for DELETE /api/v4/posts/{id}: one field, status. */
public record StatusResponse(String status) {

	public static StatusResponse ok() {
		return new StatusResponse("OK");
	}
}
