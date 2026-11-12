package com.harsha.minichat.post.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Recorded request for POST /api/v4/posts: channel_id, message, root_id, pending_post_id, user_id, create_at,
 * update_at, file_ids, props, metadata, reply_count. Only the first four are used. user_id and the timestamps
 * are ignored on purpose: the author is the logged-in user and the server sets the time.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreatePostRequest(
		@NotBlank String channelId,
		@NotBlank @Size(max = 16383) String message,
		@Size(max = 26) String rootId,
		@Size(max = 64) String pendingPostId) {
}
