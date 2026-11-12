package com.harsha.minichat.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Recorded request fields for POST /api/v4/users: email, username, password.
 * The length and character rules are my own choice; REA records field names and types, not rules.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateUserRequest(
		@NotBlank @Email @Size(max = 128) String email,
		@NotBlank @Size(min = 3, max = 64) @Pattern(regexp = "[a-z0-9._-]+", message = "may only contain lowercase letters, digits, dot, dash and underscore") String username,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
