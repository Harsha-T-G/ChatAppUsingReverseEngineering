package com.harsha.minichat.channel.dto;

import com.harsha.minichat.channel.Channel;

public record ChannelResponse(
		String id,
		long createAt,
		long updateAt,
		long deleteAt,
		String teamId,
		String type,
		String displayName,
		String name,
		String header,
		String purpose,
		long lastPostAt,
		long totalMsgCount,
		String creatorId,
		String schemeId,
		boolean groupConstrained) {

	public static ChannelResponse from(Channel channel) {
		return new ChannelResponse(
				channel.getId(),
				channel.getCreateAt(),
				channel.getUpdateAt(),
				channel.getDeleteAt(),
				channel.getTeamId(),
				channel.getType(),
				channel.getDisplayName(),
				channel.getName(),
				channel.getHeader(),
				channel.getPurpose(),
				channel.getLastPostAt(),
				channel.getTotalMsgCount(),
				channel.getCreatorId(),
				"",
				false);
	}
}
