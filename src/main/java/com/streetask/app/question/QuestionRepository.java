package com.streetask.app.question;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.streetask.app.model.Question;

public interface QuestionRepository extends CrudRepository<Question, UUID> {

	Iterable<Question> findByCreatorId(UUID creatorId);

	Iterable<Question> findByEventId(UUID eventId);

	Iterable<Question> findByEventIdOrderByCreatedAtAsc(UUID eventId);

	Iterable<Question> findByLocationLatitudeBetweenAndLocationLongitudeBetween(Double minLatitude,
			Double maxLatitude,
			Double minLongitude,
			Double maxLongitude);

	Iterable<Question> findByActive(Boolean active);

	Iterable<Question> findByCreatorIdAndActive(UUID creatorId, Boolean active);

	Iterable<Question> findByEventIdAndActive(UUID eventId, Boolean active);

	Iterable<Question> findByCreatorIdAndEventId(UUID creatorId, UUID eventId);

	Iterable<Question> findByCreatorIdAndEventIdAndActive(UUID creatorId, UUID eventId, Boolean active);

	Iterable<Question> findAllByActiveTrueAndExpiresAtBefore(Instant now);

	long countByCreatorId(UUID creatorId);

	boolean existsByLocationLatitudeBetweenAndLocationLongitudeBetween(
			Double minLatitude,
			Double maxLatitude,
			Double minLongitude,
			Double maxLongitude);

}
