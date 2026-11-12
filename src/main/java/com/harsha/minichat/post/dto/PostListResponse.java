package com.harsha.minichat.post.dto;

import java.util.List;
import java.util.Map;

/**
 * Recorded response for GET /api/v4/channels/{id}/posts: order (array), posts (object), next_post_id,
 * prev_post_id, first_inaccessible_post_time. Inferred: order lists post ids newest first, and posts maps
 * each id to its post.
 */
public record PostListResponse(
		List<String> order,
		Map<String, PostResponse> posts,
		String nextPostId,
		String prevPostId,
		long firstInaccessiblePostTime) {
}
