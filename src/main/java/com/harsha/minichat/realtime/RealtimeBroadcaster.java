package com.harsha.minichat.realtime;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.harsha.minichat.post.dto.PostResponse;

import tools.jackson.databind.json.JsonMapper;

/**
 * Turns a committed post change into the recorded event shape:
 * {event, data, broadcast: {channel_id, team_id, user_id, omit_users, connection_id, omit_connection_id}, seq}.
 * In the recording data.post is a string that holds the post as JSON, so it is sent the same way.
 */
@Component
public class RealtimeBroadcaster {

	private final RealtimeHandler handler;
	private final JsonMapper json;

	public RealtimeBroadcaster(RealtimeHandler handler, JsonMapper json) {
		this.handler = handler;
		this.json = json;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPostEvent(PostEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("post", json.writeValueAsString(PostResponse.from(event.post())));
		switch (event.type()) {
			case POSTED -> {
				data.put("channel_display_name", event.channel().getDisplayName());
				data.put("channel_name", event.channel().getName());
				data.put("channel_type", event.channel().getType());
				data.put("sender_name", "@" + event.actorName());
				data.put("set_online", true);
				data.put("team_id", event.channel().getTeamId());
			}
			case DELETED -> data.put("delete_by", event.actorId());
			case EDITED -> {
				// post only
			}
		}

		Map<String, Object> broadcast = new LinkedHashMap<>();
		broadcast.put("omit_users", null);
		broadcast.put("user_id", "");
		broadcast.put("channel_id", event.channel().getId());
		broadcast.put("team_id", event.channel().getTeamId());
		broadcast.put("connection_id", "");
		broadcast.put("omit_connection_id", "");
		if (event.type() == PostEvent.Type.DELETED) {
			// Recorded only on the delete event. REA keeps types, not values; true because the frame carries the deleted text.
			broadcast.put("contains_sensitive_data", true);
		}

		handler.broadcast(event.type().eventName(), data, broadcast, event.recipientIds());
	}
}
