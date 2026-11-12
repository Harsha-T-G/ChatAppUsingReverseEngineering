package com.harsha.minichat.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

/**
 * Recorded request fields for POST /api/v4/users/login: id, password, token, device_id.
 * The recording only contains the automatic login right after sign-up, which sends the user id.
 * login_id (email or username) is my addition so a person can log in by typing their email.
 * token and device_id are accepted and ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(String id, String loginId, @NotBlank String password, String token, String deviceId) {
}
