package com.harsha.minichat.channel;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harsha.minichat.channel.dto.CreateChannelRequest;
import com.harsha.minichat.common.ApiException;
import com.harsha.minichat.common.Ids;
import com.harsha.minichat.team.TeamService;

@Service
public class ChannelService {

	/** REA records types, not values, so the code for an "open" channel is my own choice. */
	private static final String DEFAULT_TYPE = "O";

	private final ChannelRepository channels;
	private final TeamService teamService;

	public ChannelService(ChannelRepository channels, TeamService teamService) {
		this.channels = channels;
		this.teamService = teamService;
	}

	@Transactional
	public Channel create(String userId, CreateChannelRequest request) {
		teamService.requireMembership(request.teamId(), userId);
		if (channels.existsByTeamIdAndName(request.teamId(), request.name())) {
			throw ApiException.badRequest("minichat.channel.name_exists", "A channel with that name already exists on this team.");
		}
		String type = isBlank(request.type()) ? DEFAULT_TYPE : request.type();
		Channel channel = new Channel(Ids.newId(), System.currentTimeMillis(), request.teamId(), type, request.name(),
				request.displayName().trim(), orEmpty(request.header()), orEmpty(request.purpose()), userId);
		return channels.save(channel);
	}

	@Transactional(readOnly = true)
	public List<Channel> listForTeam(String userId, String teamId) {
		teamService.requireMembership(teamId, userId);
		return channels.findByTeamIdAndDeleteAtOrderByDisplayName(teamId, 0);
	}

	/** Returns the channel if the user belongs to its team; 404 if it does not exist, 403 otherwise. */
	@Transactional(readOnly = true)
	public Channel requireAccess(String channelId, String userId) {
		Channel channel = channels.findById(channelId)
				.filter(found -> found.getDeleteAt() == 0)
				.orElseThrow(() -> ApiException.notFound("minichat.channel.not_found", "Unable to find the channel."));
		teamService.requireMembership(channel.getTeamId(), userId);
		return channel;
	}

	/** Called by the post service inside its transaction when a message is saved. */
	@Transactional
	public void recordNewPost(String channelId, long postedAt) {
		channels.recordNewPost(channelId, postedAt);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static String orEmpty(String value) {
		return value == null ? "" : value;
	}
}
