package com.harsha.minichat.team.dto;

import com.harsha.minichat.team.Team;

public record TeamResponse(
		String id,
		long createAt,
		long updateAt,
		long deleteAt,
		String displayName,
		String name,
		String description,
		String email,
		String type,
		String companyName,
		String allowedDomains,
		String inviteId,
		boolean allowOpenInvite,
		String schemeId,
		boolean groupConstrained) {

	public static TeamResponse from(Team team) {
		return new TeamResponse(
				team.getId(),
				team.getCreateAt(),
				team.getUpdateAt(),
				team.getDeleteAt(),
				team.getDisplayName(),
				team.getName(),
				team.getDescription(),
				team.getEmail(),
				team.getType(),
				team.getCompanyName(),
				team.getAllowedDomains(),
				team.getInviteId(),
				team.isAllowOpenInvite(),
				"",
				false);
	}
}
