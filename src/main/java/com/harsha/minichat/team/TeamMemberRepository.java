package com.harsha.minichat.team;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {

	boolean existsByTeamIdAndUserId(String teamId, String userId);

	@Query("select m.userId from TeamMember m where m.teamId = :teamId")
	List<String> findUserIdsByTeamId(@Param("teamId") String teamId);
}
