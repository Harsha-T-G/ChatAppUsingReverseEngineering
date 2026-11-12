package com.harsha.minichat.channel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelRepository extends JpaRepository<Channel, String> {

	boolean existsByTeamIdAndName(String teamId, String name);

	List<Channel> findByTeamIdAndDeleteAtOrderByDisplayName(String teamId, long deleteAt);

	/** Counts a new message in one SQL statement, so two people posting at once cannot lose a count. */
	@Modifying
	@Query("""
			update Channel c
			set c.totalMsgCount = c.totalMsgCount + 1,
			    c.lastPostAt = case when c.lastPostAt > :postedAt then c.lastPostAt else :postedAt end
			where c.id = :channelId
			""")
	void recordNewPost(@Param("channelId") String channelId, @Param("postedAt") long postedAt);
}
