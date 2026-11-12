package com.harsha.minichat.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, String> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	@Query("""
			select u from User u
			where u.deleteAt = 0
			  and u.id in (select m.userId from TeamMember m where m.teamId = :teamId)
			order by u.username
			""")
	List<User> findMembersOfTeam(@Param("teamId") String teamId, Pageable page);
}
