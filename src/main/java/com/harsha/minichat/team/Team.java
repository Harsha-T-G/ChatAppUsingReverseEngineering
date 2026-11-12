package com.harsha.minichat.team;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team {

	@Id
	private String id;
	private long createAt;
	private long updateAt;
	private long deleteAt;
	private String name;
	private String displayName;
	private String description;
	private String email;
	private String type;
	private String companyName;
	private String allowedDomains;
	private String inviteId;
	private boolean allowOpenInvite;

	protected Team() {
	}

	public Team(String id, long now, String name, String displayName, String description, String email, String type, String inviteId) {
		this.id = id;
		this.createAt = now;
		this.updateAt = now;
		this.deleteAt = 0;
		this.name = name;
		this.displayName = displayName;
		this.description = description;
		this.email = email;
		this.type = type;
		this.companyName = "";
		this.allowedDomains = "";
		this.inviteId = inviteId;
		this.allowOpenInvite = false;
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

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}

	public String getEmail() {
		return email;
	}

	public String getType() {
		return type;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getAllowedDomains() {
		return allowedDomains;
	}

	public String getInviteId() {
		return inviteId;
	}

	public boolean isAllowOpenInvite() {
		return allowOpenInvite;
	}
}
