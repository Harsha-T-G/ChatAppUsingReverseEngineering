package com.harsha.minichat.post;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harsha.minichat.auth.AuthInterceptor;
import com.harsha.minichat.post.dto.CreatePostRequest;
import com.harsha.minichat.post.dto.PatchPostRequest;
import com.harsha.minichat.post.dto.PostListResponse;
import com.harsha.minichat.post.dto.PostResponse;
import com.harsha.minichat.common.StatusResponse;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v4")
public class PostController {

	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	/** Recorded: POST /api/v4/posts -> 201. */
	@PostMapping("/posts")
	public ResponseEntity<PostResponse> create(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@Valid @RequestBody CreatePostRequest request) {
		Post post = postService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(post));
	}

	/**
	 * Recorded: GET /api/v4/channels/{id}/posts -> 200, with a "since" query parameter.
	 * page and per_page were recorded on other list endpoints and are reused here.
	 */
	@GetMapping("/channels/{channelId}/posts")
	public PostListResponse list(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@PathVariable String channelId,
			@RequestParam(required = false) Long since,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(name = "per_page", defaultValue = "60") int perPage) {
		List<Post> found = postService.list(userId, channelId, since, page, perPage);
		Map<String, PostResponse> byId = new LinkedHashMap<>();
		for (Post post : found) {
			byId.put(post.getId(), PostResponse.from(post));
		}
		return new PostListResponse(List.copyOf(byId.keySet()), byId, "", "", 0);
	}

	/** Recorded: PUT /api/v4/posts/{id}/patch -> 200. */
	@PutMapping("/posts/{postId}/patch")
	public PostResponse edit(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@PathVariable String postId,
			@Valid @RequestBody PatchPostRequest request) {
		return PostResponse.from(postService.edit(userId, postId, request));
	}

	/** Recorded: DELETE /api/v4/posts/{id} -> 200 with {"status": ...}. */
	@DeleteMapping("/posts/{postId}")
	public StatusResponse delete(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@PathVariable String postId) {
		postService.delete(userId, postId);
		return StatusResponse.ok();
	}
}
