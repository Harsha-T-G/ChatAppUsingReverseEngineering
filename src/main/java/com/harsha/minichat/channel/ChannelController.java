package com.harsha.minichat.channel;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harsha.minichat.auth.AuthInterceptor;
import com.harsha.minichat.channel.dto.ChannelResponse;
import com.harsha.minichat.channel.dto.CreateChannelRequest;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v4")
public class ChannelController {

	private final ChannelService channelService;

	public ChannelController(ChannelService channelService) {
		this.channelService = channelService;
	}

	/** Recorded: POST /api/v4/channels -> 201. */
	@PostMapping("/channels")
	public ResponseEntity<ChannelResponse> create(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@Valid @RequestBody CreateChannelRequest request) {
		Channel channel = channelService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ChannelResponse.from(channel));
	}

	/** Recorded: GET /api/v4/users/me/teams/{id}/channels -> 200 with an array. */
	@GetMapping("/users/me/teams/{teamId}/channels")
	public List<ChannelResponse> myChannels(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@PathVariable String teamId) {
		return channelService.listForTeam(userId, teamId).stream().map(ChannelResponse::from).toList();
	}

	/** Recorded: GET /api/v4/channels/{id} -> 200. */
	@GetMapping("/channels/{channelId}")
	public ChannelResponse get(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@PathVariable String channelId) {
		return ChannelResponse.from(channelService.requireAccess(channelId, userId));
	}
}
