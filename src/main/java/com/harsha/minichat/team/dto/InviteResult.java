package com.harsha.minichat.team.dto;

import com.harsha.minichat.common.ApiError;

/**
 * Recorded response item for POST /api/v4/teams/{id}/invite/email: email and error.
 * error is null when the person was added.
 */
public record InviteResult(String email, ApiError error) {
}
