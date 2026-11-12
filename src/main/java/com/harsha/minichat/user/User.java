package com.harsha.minichat.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	private String id;
	private long createAt;
	private long updateAt;
	private long deleteAt;
	private String username;
	private String email;
	private String passwordHash;
	private String firstName;
	private String lastName;
	private String nickname;
	private String roles;
	private String locale;

	protected User() {
	}

	public User(String id, long now, String username, String email, String passwordHash, String roles) {
		this.id = id;
		this.createAt = now;
		this.updateAt = now;
		this.deleteAt = 0;
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.firstName = "";
		this.lastName = "";
		this.nickname = "";
		this.roles = roles;
		this.locale = "en";
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

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getNickname() {
		return nickname;
	}

	public String getRoles() {
		return roles;
	}

	public String getLocale() {
		return locale;
	}
}
