package com.harsha.minichat.auth;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harsha.minichat.common.Ids;

/**
 * Opens, checks and ends login sessions. A token stops working when the person logs out
 * or when it is older than minichat.session.ttl-days.
 */
@Service
public class SessionService {

	private final SessionRepository sessions;
	private final long ttlMillis;

	public SessionService(SessionRepository sessions, @Value("${minichat.session.ttl-days:30}") long ttlDays) {
		this.sessions = sessions;
		this.ttlMillis = Duration.ofDays(ttlDays).toMillis();
	}

	@Transactional
	public Session open(String userId) {
		return sessions.save(new Session(Ids.newId(), userId, System.currentTimeMillis()));
	}

	/** The session for this token if it exists and has not expired. Expired sessions are removed. */
	@Transactional
	public Optional<Session> findValid(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		Optional<Session> found = sessions.findById(token);
		if (found.isPresent() && System.currentTimeMillis() - found.get().getCreateAt() > ttlMillis) {
			sessions.delete(found.get());
			return Optional.empty();
		}
		return found;
	}

	@Transactional
	public void end(String token) {
		sessions.deleteById(token);
	}
}
