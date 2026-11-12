package com.harsha.minichat.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.harsha.minichat.common.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Checks "Authorization: Bearer <token>" on every API call except sign-up and login,
 * and puts the logged-in user's id on the request for the controllers.
 *
 * REA never records headers or cookies, so this mechanism is not from the recording.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	public static final String USER_ID = "minichat.userId";
	public static final String TOKEN = "minichat.token";

	private static final String BEARER = "Bearer ";

	private final SessionService sessions;

	public AuthInterceptor(SessionService sessions) {
		this.sessions = sessions;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isPublic(request)) {
			return true;
		}
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER)) {
			throw ApiException.unauthorized("minichat.session.missing", "Log in first, then send the token as 'Authorization: Bearer <token>'.");
		}
		String token = header.substring(BEARER.length()).trim();
		Session session = sessions.findValid(token)
				.orElseThrow(() -> ApiException.unauthorized("minichat.session.invalid", "Invalid or expired session, please log in again."));
		request.setAttribute(USER_ID, session.getUserId());
		request.setAttribute(TOKEN, session.getToken());
		return true;
	}

	private boolean isPublic(HttpServletRequest request) {
		if (!"POST".equals(request.getMethod())) {
			return false;
		}
		String path = request.getRequestURI();
		return path.equals("/api/v4/users") || path.equals("/api/v4/users/login");
	}
}
