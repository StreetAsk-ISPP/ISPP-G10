package com.streetask.app.answer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.streetask.app.model.Answer;

public interface AnswerRepository extends CrudRepository<Answer, UUID> {

	Iterable<Answer> findByQuestionId(UUID questionId);

	List<Answer> findByQuestionIdOrderByUpvotesDescCreatedAtDesc(UUID questionId);

	List<Answer> findByQuestionIdOrderByCreatedAtDesc(UUID questionId);

	List<Answer> findByQuestionIdOrderByUpvotesDescCreatedAtDesc(UUID questionId, Pageable pageable);

	List<Answer> findByQuestionIdOrderByCreatedAtDesc(UUID questionId, Pageable pageable);

	Iterable<Answer> findByUserId(UUID userId);

	Iterable<Answer> findByIsVerified(Boolean isVerified);

	Iterable<Answer> findByUserIdAndIsVerified(UUID userId, Boolean isVerified);

	Iterable<Answer> findByQuestionIdAndIsVerified(UUID questionId, Boolean isVerified);

	Iterable<Answer> findByQuestionIdAndUserId(UUID questionId, UUID userId);

	Iterable<Answer> findByQuestionIdAndUserIdAndIsVerified(UUID questionId, UUID userId, Boolean isVerified);
	
	// Custom query to aggregate upvotes and downvotes by user IDs
	@Query("SELECT a.user.id, COALESCE(SUM(a.upvotes), 0), COALESCE(SUM(a.downvotes), 0) "
			+ "FROM Answer a WHERE a.user.id IN :userIds GROUP BY a.user.id")
	List<Object[]> aggregateVotesByUserIds(Collection<UUID> userIds);

	long countByUserId(UUID userId);

	@Modifying
	@Query("UPDATE Answer a SET a.rewardClaimed = true WHERE a.id = :answerId AND (a.rewardClaimed = false OR a.rewardClaimed IS NULL)")
	int markRewardClaimedIfUnclaimed(@Param("answerId") UUID answerId);

}
