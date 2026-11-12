package com.harsha.minichat.post.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
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
		@JsonInclude(JsonInclude.Include.NON_NULL) String remoteId,
		long replyCount,
		long lastReplyAt,
		List<String> participants,
		Map<String, Object> metadata) {

	public static PostResponse from(Post post) {
		return build(post, "");
	}

	/**
	 * The edit response. In the recording it has every field of a new post except remote_id.
	 * Found by pointing REA at this backend and comparing with the original recording.
	 */
	public static PostResponse edited(Post post) {
		return build(post, null);
	}

	private static PostResponse build(Post post, String remoteId) {
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
				remoteId,
				0,
				0,
				null,
				Map.of());
	}
}
