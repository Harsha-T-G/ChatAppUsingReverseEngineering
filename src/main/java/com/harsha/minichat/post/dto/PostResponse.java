package com.harsha.minichat.post.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.harsha.minichat.post.Post;

/**
 * Post as recorded in the responses. Fields this mini version does not support yet (pinning, files, hashtags,
 * threads, props) are returned with empty values so the shape still matches.
 */
public record PostResponse(
		String id,
		long createAt,
		long updateAt,
		long editAt,
		long deleteAt,
		@JsonProperty("is_pinned") boolean pinned,
		String userId,
		String channelId,
		String rootId,
		String originalId,
		String message,
		String type,
		Map<String, Object> props,
		String hashtags,
		List<String> fileIds,
		String pendingPostId,
		String remoteId,
		long replyCount,
		long lastReplyAt,
		List<String> participants,
		Map<String, Object> metadata) {

	public static PostResponse from(Post post) {
		return new PostResponse(
				post.getId(),
				post.getCreateAt(),
				post.getUpdateAt(),
				post.getEditAt(),
				post.getDeleteAt(),
				false,
				post.getUserId(),
				post.getChannelId(),
				post.getRootId(),
				"",
				post.getMessage(),
				"",
				Map.of(),
				"",
				List.of(),
				post.getPendingPostId(),
				"",
				0,
				0,
				null,
				Map.of());
	}
}
