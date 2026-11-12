package com.harsha.minichat.post;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harsha.minichat.channel.ChannelService;
import com.harsha.minichat.common.ApiException;
import com.harsha.minichat.common.Ids;
import com.harsha.minichat.post.dto.CreatePostRequest;
import com.harsha.minichat.post.dto.PatchPostRequest;

@Service
public class PostService {

	static final int DEFAULT_PAGE_SIZE = 60;
	static final int MAX_PAGE_SIZE = 200;

	private final PostRepository posts;
	private final ChannelService channelService;

	public PostService(PostRepository posts, ChannelService channelService) {
		this.posts = posts;
		this.channelService = channelService;
	}

	/** Saves the message and updates the channel's counters in one transaction. */
	@Transactional
	public Post create(String userId, CreatePostRequest request) {
		channelService.requireAccess(request.channelId(), userId);
		String rootId = request.rootId() == null ? "" : request.rootId();
		if (!rootId.isEmpty()) {
			requireRootInSameChannel(rootId, request.channelId());
		}
		long now = System.currentTimeMillis();
		String pendingPostId = request.pendingPostId() == null ? "" : request.pendingPostId();
		Post post = posts.save(new Post(Ids.newId(), now, userId, request.channelId(), rootId, request.message(), pendingPostId));
		channelService.recordNewPost(request.channelId(), now);
		return post;
	}

	/**
	 * Messages of a channel, newest first. With since, returns everything changed after that time
	 * (including deleted posts, so a client can remove them from its screen).
	 */
	@Transactional(readOnly = true)
	public List<Post> list(String userId, String channelId, Long since, int page, int perPage) {
		channelService.requireAccess(channelId, userId);
		PageRequest pageRequest = PageRequest.of(Math.max(page, 0), clampPageSize(perPage));
		if (since != null && since > 0) {
			return posts.findChangedSince(channelId, since, pageRequest);
		}
		return posts.findVisibleInChannel(channelId, pageRequest);
	}

	@Transactional
	public Post edit(String userId, String postId, PatchPostRequest request) {
		Post post = requireOwnPost(userId, postId, "edit");
		post.edit(request.message(), System.currentTimeMillis());
		return post;
	}

	@Transactional
	public void delete(String userId, String postId) {
		Post post = requireOwnPost(userId, postId, "delete");
		post.markDeleted(System.currentTimeMillis());
	}

	private Post requireOwnPost(String userId, String postId, String action) {
		Post post = posts.findById(postId)
				.filter(found -> !found.isDeleted())
				.orElseThrow(() -> ApiException.notFound("minichat.post.not_found", "Unable to find the post."));
		// Check channel access first so a non-member cannot learn who wrote a post.
		channelService.requireAccess(post.getChannelId(), userId);
		if (!post.getUserId().equals(userId)) {
			throw ApiException.forbidden("minichat.post.not_author", "You can only " + action + " your own posts.");
		}
		return post;
	}

	private void requireRootInSameChannel(String rootId, String channelId) {
		boolean valid = posts.findById(rootId)
				.filter(root -> !root.isDeleted() && root.getChannelId().equals(channelId))
				.isPresent();
		if (!valid) {
			throw ApiException.badRequest("minichat.post.root_invalid", "The post you are replying to does not exist in this channel.");
		}
	}

	private static int clampPageSize(int perPage) {
		if (perPage <= 0) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(perPage, MAX_PAGE_SIZE);
	}
}
