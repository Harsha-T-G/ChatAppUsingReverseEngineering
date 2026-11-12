package com.harsha.minichat.realtime;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.harsha.minichat.auth.Session;
import com.harsha.minichat.auth.SessionRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Live updates on /api/v4/websocket.
 *
 * Recorded frame shapes: the client sends {action, seq, data}; the server answers {status, seq_reply} and
 * pushes {event, data, broadcast, seq}. REA does not record values, headers or cookies, so the action
 * and event names and the way a connection proves who it is are not from the recording: the first frame
 * must be {"action": "authentication_challenge", "seq": 1, "data": {"token": "<login token>"}}.
 */
@Component
public class RealtimeHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(RealtimeHandler.class);
	private static final String AUTH_ACTION = "authentication_challenge";
	private static final int SEND_TIME_LIMIT_MS = 5_000;
	private static final int SEND_BUFFER_LIMIT_BYTES = 256 * 1024;

	private final Map<String, Connection> connections = new ConcurrentHashMap<>();
	private final SessionRepository sessions;
	private final JsonMapper json;

	public RealtimeHandler(SessionRepository sessions, JsonMapper json) {
		this.sessions = sessions;
		this.json = json;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		// The decorator makes sending safe when several threads push events to the same connection.
		WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT_BYTES);
		connections.put(session.getId(), new Connection(safe));
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		Connection connection = connections.get(session.getId());
		if (connection == null) {
			return;
		}
		JsonNode frame;
		try {
			frame = json.readTree(message.getPayload());
		} catch (JacksonException ex) {
			reject(connection, 0, "Frames must be JSON.");
			return;
		}
		long seq = frame.path("seq").asLong(0);
		String action = frame.path("action").asString("");

		if (AUTH_ACTION.equals(action)) {
			String token = frame.path("data").path("token").asString("");
			Optional<Session> found = token.isEmpty() ? Optional.empty() : sessions.findById(token);
			if (found.isEmpty()) {
				reject(connection, seq, "Invalid or expired session, please log in again.");
				return;
			}
			connection.token = token;
			connection.userId = found.get().getUserId();
			reply(connection, seq, "OK", null);
			return;
		}
		if (connection.userId == null) {
			reject(connection, seq, "Send authentication_challenge with your token first.");
			return;
		}
		// Other client actions (for example "user is typing") are accepted but not acted on yet.
		reply(connection, seq, "OK", null);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		connections.remove(session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		connections.remove(session.getId());
	}

	/** Sends one event to every authenticated connection that belongs to one of the given users. */
	public void broadcast(String event, Map<String, Object> data, Map<String, Object> broadcast, Collection<String> recipientIds) {
		Set<String> recipients = Set.copyOf(recipientIds);
		for (Connection connection : connections.values()) {
			String userId = connection.userId;
			if (userId == null || !recipients.contains(userId)) {
				continue;
			}
			Map<String, Object> frame = new LinkedHashMap<>();
			frame.put("event", event);
			frame.put("data", data);
			frame.put("broadcast", broadcast);
			frame.put("seq", connection.nextSeq.getAndIncrement());
			send(connection, frame);
		}
	}

	/** Called on logout: connections opened with that token stop receiving updates. */
	public void closeForToken(String token) {
		for (Connection connection : connections.values()) {
			if (token.equals(connection.token)) {
				connection.userId = null;
				connections.remove(connection.session.getId());
				try {
					connection.session.close(CloseStatus.NORMAL);
				} catch (IOException | RuntimeException ignored) {
					// already closed
				}
			}
		}
	}

	private void reply(Connection connection, long seq, String status, String error) {
		Map<String, Object> frame = new LinkedHashMap<>();
		frame.put("status", status);
		frame.put("seq_reply", seq);
		if (error != null) {
			frame.put("error", error);
		}
		send(connection, frame);
	}

	private void reject(Connection connection, long seq, String reason) throws IOException {
		reply(connection, seq, "FAIL", reason);
		connections.remove(connection.session.getId());
		connection.session.close(CloseStatus.POLICY_VIOLATION);
	}

	private void send(Connection connection, Map<String, Object> frame) {
		try {
			connection.session.sendMessage(new TextMessage(json.writeValueAsString(frame)));
		} catch (IOException | RuntimeException ex) {
			// A slow or broken client must not stop the others from getting the event.
			log.debug("Dropping live-update connection {}: {}", connection.session.getId(), ex.toString());
			connections.remove(connection.session.getId());
			try {
				connection.session.close(CloseStatus.SESSION_NOT_RELIABLE);
			} catch (IOException | RuntimeException ignored) {
				// already closed
			}
		}
	}

	private static final class Connection {

		private final WebSocketSession session;
		private final AtomicLong nextSeq = new AtomicLong(1);
		private volatile String userId;
		private volatile String token;

		private Connection(WebSocketSession session) {
			this.session = session;
		}
	}
}
