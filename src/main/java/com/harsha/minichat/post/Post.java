package com.harsha.minichat.post;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
public class Post {

	@Id
	private String id;
	private long createAt;
	private long updateAt;
	private long editAt;
	private long deleteAt;
	private String userId;
	private String channelId;
	private String rootId;
	private String message;
	private String pendingPostId;

	protected Post() {
	}

	public Post(String id, long now, String userId, String channelId, String rootId, String message, String pendingPostId) {
		this.id = id;
		this.createAt = now;
		this.updateAt = now;
		this.editAt = 0;
		this.deleteAt = 0;
		this.userId = userId;
		this.channelId = channelId;
		this.rootId = rootId;
		this.message = message;
		this.pendingPostId = pendingPostId;
	}

	public void edit(String newMessage, long now) {
		this.message = newMessage;
		this.editAt = now;
		this.updateAt = now;
	}

	/** Soft delete: the row stays, delete_at records when it was removed. */
	public void markDeleted(long now) {
		this.deleteAt = now;
		this.updateAt = now;
	}

	public boolean isDeleted() {
		return deleteAt != 0;
	}

	public String getId() {
		return id;
	}

	public long getCreateAt() {
		return createAt;
	}

	public long getUpdateAt() {
		return updateAt;
	}

	public long getEditAt() {
		return editAt;
	}

	public long getDeleteAt() {
		return deleteAt;
	}

	public String getUserId() {
		return userId;
	}

	public String getChannelId() {
		return channelId;
	}

	public String getRootId() {
		return rootId;
	}

	public String getMessage() {
		return message;
	}

	public String getPendingPostId() {
		return pendingPostId;
	}
}
