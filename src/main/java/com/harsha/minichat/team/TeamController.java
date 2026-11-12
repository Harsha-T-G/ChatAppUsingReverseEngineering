package com.harsha.minichat.team;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harsha.minichat.auth.AuthInterceptor;
import com.harsha.minichat.team.dto.CreateTeamRequest;
import com.harsha.minichat.team.dto.InviteResult;
import com.harsha.minichat.team.dto.TeamResponse;
import com.harsha.minichat.user.UserService;
import com.harsha.minichat.user.dto.PublicUserResponse;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v4")
public class TeamController {

	private final TeamService teamService;
	private final UserService userService;

	public TeamController(TeamService teamService, UserService userService) {
		this.teamService = teamService;
		this.userService = userService;
	}

	/** Recorded: POST /api/v4/teams -> 201. */
	@PostMapping("/teams")
	public ResponseEntity<TeamResponse> create(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@Valid @RequestBody CreateTeamRequest request) {
		Team team = teamService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(team));
	}

	/** Recorded: POST /api/v4/teams/{id}/invite/email -> 200, body is an array of emails. */
	@PostMapping("/teams/{teamId}/invite/email")
	public List<InviteResult> invite(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@PathVariable String teamId,
			@RequestBody List<String> emails) {
		return teamService.inviteByEmail(userId, teamId, emails);
	}

	/**
	 * Recorded: GET /api/v4/users -> 200 with an array, query page and per_page.
	 * in_team is my addition and is required here, so people can only list the members of their own teams.
	 */
	@GetMapping("/users")
	public List<PublicUserResponse> teamMembers(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@RequestParam(name = "in_team") String teamId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(name = "per_page", defaultValue = "60") int perPage) {
		teamService.requireMembership(teamId, userId);
		return userService.listTeamMembers(teamId, page, perPage).stream().map(PublicUserResponse::from).toList();
	}

	/** Recorded: GET /api/v4/users/me/teams -> 200 with an array. */
	@GetMapping("/users/me/teams")
	public List<TeamResponse> myTeams(@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId) {
		return teamService.listForUser(userId).stream().map(TeamResponse::from).toList();
	}
}
