package com.harsha.minichat.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class Session {

	@Id
	private String token;
	private String userId;
	private long createAt;

	protected Session() {
	}

	public Session(String token, String userId, long createAt) {
		this.token = token;
		this.userId = userId;
		this.createAt = createAt;
	}

	public String getToken() {
		return token;
	}

	public String getUserId() {
		return userId;
	}

	public long getCreateAt() {
		return createAt;
	}
}
