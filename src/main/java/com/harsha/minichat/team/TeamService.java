package com.harsha.minichat.team;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harsha.minichat.common.ApiException;
import com.harsha.minichat.common.Ids;
import com.harsha.minichat.team.dto.CreateTeamRequest;
import com.harsha.minichat.user.User;
import com.harsha.minichat.user.UserService;

@Service
public class TeamService {

	/** REA records types, not values, so the code for an "open" team is my own choice. */
	private static final String DEFAULT_TYPE = "O";
	private static final String CREATOR_ROLES = "team_user team_admin";

	private final TeamRepository teams;
	private final TeamMemberRepository members;
	private final UserService userService;

	public TeamService(TeamRepository teams, TeamMemberRepository members, UserService userService) {
		this.teams = teams;
		this.members = members;
		this.userService = userService;
	}

	/** Creates the team and makes the creator its first member, in one transaction. */
	@Transactional
	public Team create(String userId, CreateTeamRequest request) {
		if (teams.existsByName(request.name())) {
			throw ApiException.badRequest("minichat.team.name_exists", "A team with that name already exists.");
		}
		User creator = userService.get(userId);
		long now = System.currentTimeMillis();
		String type = isBlank(request.type()) ? DEFAULT_TYPE : request.type();
		String description = request.description() == null ? "" : request.description();
		Team team = teams.save(new Team(Ids.newId(), now, request.name(), request.displayName().trim(),
				description, creator.getEmail(), type, Ids.newId()));
		members.save(new TeamMember(Ids.newId(), team.getId(), userId, CREATOR_ROLES, now));
		return team;
	}

	@Transactional(readOnly = true)
	public List<Team> listForUser(String userId) {
		return teams.findAllForUser(userId);
	}

	/** Returns the team if the user is a member; 404 if it does not exist, 403 if they are not in it. */
	@Transactional(readOnly = true)
	public Team requireMembership(String teamId, String userId) {
		Team team = teams.findById(teamId)
				.filter(found -> found.getDeleteAt() == 0)
				.orElseThrow(() -> ApiException.notFound("minichat.team.not_found", "Unable to find the team."));
		if (!members.existsByTeamIdAndUserId(teamId, userId)) {
			throw ApiException.forbidden("minichat.team.not_member", "You are not a member of this team.");
		}
		return team;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
