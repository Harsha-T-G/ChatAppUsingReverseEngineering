package com.harsha.minichat.post.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Recorded request for PUT /api/v4/posts/{id}/patch sends the whole post back. Only message can be changed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PatchPostRequest(@NotBlank @Size(max = 16383) String message) {
}
