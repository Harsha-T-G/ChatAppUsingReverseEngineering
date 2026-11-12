package com.harsha.minichat.user.dto;

import com.harsha.minichat.user.User;

/**
 * User as returned by sign-up, login and "me". Field names come from the recording; the password is never returned.
 */
public record UserResponse(
		String id,
		long createAt,
		long updateAt,
		long deleteAt,
		String username,
		String authService,
		String email,
		String nickname,
		String firstName,
		String lastName,
		String position,
		String roles,
		String locale) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getCreateAt(),
				user.getUpdateAt(),
				user.getDeleteAt(),
				user.getUsername(),
				"",
				user.getEmail(),
				user.getNickname(),
				user.getFirstName(),
				user.getLastName(),
				"",
				user.getRoles(),
				user.getLocale());
	}
}
