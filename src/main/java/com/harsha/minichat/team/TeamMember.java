package com.harsha.minichat.team;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "team_members")
public class TeamMember {

	@Id
	private String id;
	private String teamId;
	private String userId;
	private String roles;
	private long createAt;

	protected TeamMember() {
	}

	public TeamMember(String id, String teamId, String userId, String roles, long createAt) {
		this.id = id;
		this.teamId = teamId;
		this.userId = userId;
		this.roles = roles;
		this.createAt = createAt;
	}

	public String getId() {
		return id;
	}

	public String getTeamId() {
		return teamId;
	}

	public String getUserId() {
		return userId;
	}

	public String getRoles() {
		return roles;
	}

	public long getCreateAt() {
		return createAt;
	}
}
