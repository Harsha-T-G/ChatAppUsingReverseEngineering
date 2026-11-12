package com.harsha.minichat.realtime;

import java.util.List;

import com.harsha.minichat.channel.Channel;
import com.harsha.minichat.post.Post;

/**
 * Published inside the transaction that changes a post. It is delivered to connected clients only after
 * that transaction commits, so nobody is told about a message that was rolled back.
 */
public record PostEvent(Type type, Post post, Channel channel, String actorId, String actorName, List<String> recipientIds) {

	public enum Type {
		POSTED("posted"),
		EDITED("post_edited"),
		DELETED("post_deleted");

		private final String eventName;

		Type(String eventName) {
			this.eventName = eventName;
		}

		public String eventName() {
			return eventName;
		}
	}
}
