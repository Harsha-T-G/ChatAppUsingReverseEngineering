package com.harsha.minichat.team.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Recorded request for POST /api/v4/teams sends a whole team object. Only these fields are used;
 * the others the client sends (id, create_at, invite_id, ...) are ignored and set by the server.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateTeamRequest(
		@NotBlank @Size(min = 2, max = 64) @Pattern(regexp = "[a-z0-9-]+", message = "may only contain lowercase letters, digits and dashes") String name,
		@NotBlank @Size(max = 64) String displayName,
		@Size(max = 8) String type,
		@Size(max = 255) String description) {
}
