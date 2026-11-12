package com.harsha.minichat.post;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, String> {

	/** Newest first; id is the tie-breaker so two posts in the same millisecond keep a stable order. */
	@Query("""
			select p from Post p
			where p.channelId = :channelId and p.deleteAt = 0
			order by p.createAt desc, p.id desc
			""")
	List<Post> findVisibleInChannel(@Param("channelId") String channelId, Pageable page);

	/** Everything that changed after the given time, including deletions, so a client can catch up. */
	@Query("""
			select p from Post p
			where p.channelId = :channelId and p.updateAt > :since
			order by p.createAt desc, p.id desc
			""")
	List<Post> findChangedSince(@Param("channelId") String channelId, @Param("since") long since, Pageable page);
}
