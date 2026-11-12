package com.harsha.minichat.user;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harsha.minichat.auth.Session;
import com.harsha.minichat.auth.SessionService;
import com.harsha.minichat.common.ApiException;
import com.harsha.minichat.common.Ids;
import com.harsha.minichat.user.dto.CreateUserRequest;
import com.harsha.minichat.user.dto.LoginRequest;

@Service
public class UserService {

	private static final String DEFAULT_ROLES = "system_user";

	private final UserRepository users;
	private final SessionService sessions;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public UserService(UserRepository users, SessionService sessions) {
		this.users = users;
		this.sessions = sessions;
	}

	@Transactional
	public User create(CreateUserRequest request) {
		String username = request.username().trim().toLowerCase(Locale.ROOT);
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (users.existsByUsername(username)) {
			throw ApiException.badRequest("minichat.user.username_exists", "An account with that username already exists.");
		}
		if (users.existsByEmail(email)) {
			throw ApiException.badRequest("minichat.user.email_exists", "An account with that email already exists.");
		}
		User user = new User(Ids.newId(), System.currentTimeMillis(), username, email,
				passwordEncoder.encode(request.password()), DEFAULT_ROLES);
		return users.save(user);
	}

	/** Checks the password and opens a session. The same error is used for "no such user" and "wrong password". */
	@Transactional
	public LoginResult login(LoginRequest request) {
		Optional<User> found = findForLogin(request);
		if (found.isEmpty() || found.get().getDeleteAt() != 0
				|| !passwordEncoder.matches(request.password(), found.get().getPasswordHash())) {
			throw ApiException.unauthorized("minichat.user.login_failed", "Enter a valid email or username and password.");
		}
		User user = found.get();
		Session session = sessions.open(user.getId());
		return new LoginResult(user, session.getToken());
	}

	/** Forgets the session so the token stops working. */
	@Transactional
	public void logout(String token) {
		sessions.end(token);
	}

	@Transactional(readOnly = true)
	public User get(String userId) {
		return users.findById(userId)
				.orElseThrow(() -> ApiException.notFound("minichat.user.not_found", "Unable to find the user."));
	}

	/** Members of a team, by username. The caller must already have checked that the asker is in that team. */
	@Transactional(readOnly = true)
	public List<User> listTeamMembers(String teamId, int page, int perPage) {
		int size = perPage <= 0 ? 60 : Math.min(perPage, 200);
		return users.findMembersOfTeam(teamId, PageRequest.of(Math.max(page, 0), size));
	}

	@Transactional(readOnly = true)
	public Optional<User> findActiveByEmail(String email) {
		if (email == null) {
			return Optional.empty();
		}
		return users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).filter(user -> user.getDeleteAt() == 0);
	}

	private Optional<User> findForLogin(LoginRequest request) {
		if (request.id() != null && !request.id().isBlank()) {
			return users.findById(request.id());
		}
		if (request.loginId() == null || request.loginId().isBlank()) {
			return Optional.empty();
		}
		String loginId = request.loginId().trim().toLowerCase(Locale.ROOT);
		Optional<User> byEmail = users.findByEmail(loginId);
		return byEmail.isPresent() ? byEmail : users.findByUsername(loginId);
	}

	public record LoginResult(User user, String token) {
	}
}
