package com.harsha.minichat.team;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {

	boolean existsByTeamIdAndUserId(String teamId, String userId);
}
