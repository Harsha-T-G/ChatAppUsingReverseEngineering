package com.harsha.minichat.user;

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harsha.minichat.auth.Session;
import com.harsha.minichat.auth.SessionRepository;
import com.harsha.minichat.common.ApiException;
import com.harsha.minichat.common.Ids;
import com.harsha.minichat.user.dto.CreateUserRequest;
import com.harsha.minichat.user.dto.LoginRequest;

@Service
public class UserService {

	private static final String DEFAULT_ROLES = "system_user";

	private final UserRepository users;
	private final SessionRepository sessions;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public UserService(UserRepository users, SessionRepository sessions) {
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
		Session session = sessions.save(new Session(Ids.newId(), user.getId(), System.currentTimeMillis()));
		return new LoginResult(user, session.getToken());
	}

	@Transactional(readOnly = true)
	public User get(String userId) {
		return users.findById(userId)
				.orElseThrow(() -> ApiException.notFound("minichat.user.not_found", "Unable to find the user."));
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
