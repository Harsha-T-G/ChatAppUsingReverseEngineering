package com.harsha.minichat.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harsha.minichat.auth.AuthInterceptor;
import com.harsha.minichat.common.StatusResponse;
import com.harsha.minichat.realtime.RealtimeHandler;
import com.harsha.minichat.user.dto.CreateUserRequest;
import com.harsha.minichat.user.dto.LoginRequest;
import com.harsha.minichat.user.dto.UserResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v4/users")
public class UserController {

	/** Response header that carries the session token after login. */
	public static final String TOKEN_HEADER = "Token";

	private final UserService userService;
	private final RealtimeHandler realtimeHandler;

	public UserController(UserService userService, RealtimeHandler realtimeHandler) {
		this.userService = userService;
		this.realtimeHandler = realtimeHandler;
	}

	/** Recorded: POST /api/v4/users -> 201. */
	@PostMapping
	@SecurityRequirements
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		User user = userService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
	}

	/** Recorded: POST /api/v4/users/login -> 200 with the user as body. */
	@PostMapping("/login")
	@SecurityRequirements
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
		UserService.LoginResult result = userService.login(request);
		return ResponseEntity.ok()
				.header(TOKEN_HEADER, result.token())
				.body(UserResponse.from(result.user()));
	}

	/** Not in the recording (the person never logged out while REA was watching). Added so a token can be ended. */
	@PostMapping("/logout")
	public StatusResponse logout(@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.TOKEN) String token) {
		userService.logout(token);
		realtimeHandler.closeForToken(token);
		return StatusResponse.ok();
	}

	/** Recorded: GET /api/v4/users/me -> 200. */
	@GetMapping("/me")
	public UserResponse me(@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.USER_ID) String userId) {
		return UserResponse.from(userService.get(userId));
	}
}
