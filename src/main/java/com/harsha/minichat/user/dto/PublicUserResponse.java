package com.harsha.minichat.user.dto;

import com.harsha.minichat.user.User;

/**
 * What teammates may see about each other. Unlike UserResponse it leaves out the email address.
 */
public record PublicUserResponse(
		String id,
		long createAt,
		long updateAt,
		long deleteAt,
		String username,
		String nickname,
		String firstName,
		String lastName) {

	public static PublicUserResponse from(User user) {
		return new PublicUserResponse(
				user.getId(),
				user.getCreateAt(),
				user.getUpdateAt(),
				user.getDeleteAt(),
				user.getUsername(),
				user.getNickname(),
				user.getFirstName(),
				user.getLastName());
	}
}
