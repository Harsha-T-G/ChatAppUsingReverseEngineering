package com.harsha.minichat.team;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harsha.minichat.auth.AuthInterceptor;
import com.harsha.minichat.team.dto.CreateTeamRequest;
import com.harsha.minichat.team.dto.TeamResponse;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v4")
public class TeamController {

	private final TeamService teamService;

	public TeamController(TeamService teamService) {
		this.teamService = teamService;
	}

	/** Recorded: POST /api/v4/teams -> 201. */
	@PostMapping("/teams")
	public ResponseEntity<TeamResponse> create(
			@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId,
			@Valid @RequestBody CreateTeamRequest request) {
		Team team = teamService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(team));
	}

	/** Recorded: GET /api/v4/users/me/teams -> 200 with an array. */
	@GetMapping("/users/me/teams")
	public List<TeamResponse> myTeams(@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId) {
		return teamService.listForUser(userId).stream().map(TeamResponse::from).toList();
	}
}
