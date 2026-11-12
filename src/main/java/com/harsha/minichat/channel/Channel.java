package com.harsha.minichat.channel;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "channels")
public class Channel {

	@Id
	private String id;
	private long createAt;
	private long updateAt;
	private long deleteAt;
	private String teamId;
	private String type;
	private String name;
	private String displayName;
	private String header;
	private String purpose;
	private String creatorId;
	private long lastPostAt;
	private long totalMsgCount;

	protected Channel() {
	}

	public Channel(String id, long now, String teamId, String type, String name, String displayName, String header,
			String purpose, String creatorId) {
		this.id = id;
		this.createAt = now;
		this.updateAt = now;
		this.deleteAt = 0;
		this.teamId = teamId;
		this.type = type;
		this.name = name;
		this.displayName = displayName;
		this.header = header;
		this.purpose = purpose;
		this.creatorId = creatorId;
		this.lastPostAt = 0;
		this.totalMsgCount = 0;
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

	public long getDeleteAt() {
		return deleteAt;
	}

	public String getTeamId() {
		return teamId;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getHeader() {
		return header;
	}

	public String getPurpose() {
		return purpose;
	}

	public String getCreatorId() {
		return creatorId;
	}

	public long getLastPostAt() {
		return lastPostAt;
	}

	public long getTotalMsgCount() {
		return totalMsgCount;
	}
}
