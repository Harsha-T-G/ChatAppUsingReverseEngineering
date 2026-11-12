package com.harsha.minichat.team;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, String> {

	boolean existsByName(String name);

	@Query("""
			select t from Team t
			where t.deleteAt = 0
			  and t.id in (select m.teamId from TeamMember m where m.userId = :userId)
			order by t.displayName
			""")
	List<Team> findAllForUser(@Param("userId") String userId);
}
