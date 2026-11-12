package com.harsha.minichat.channel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Recorded request for POST /api/v4/channels sends a whole channel object. Only these fields are used;
 * the others the client sends (id, creator_id, create_at, ...) are ignored and set by the server.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateChannelRequest(
		@NotBlank String teamId,
		@NotBlank @Size(min = 1, max = 64) @Pattern(regexp = "[a-z0-9_-]+", message = "may only contain lowercase letters, digits, dashes and underscores") String name,
		@NotBlank @Size(max = 64) String displayName,
		@Size(max = 8) String type,
		@Size(max = 1024) String header,
		@Size(max = 250) String purpose) {
}
