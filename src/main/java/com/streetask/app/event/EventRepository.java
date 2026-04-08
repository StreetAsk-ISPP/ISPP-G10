package com.streetask.app.event;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import com.streetask.app.model.Event;
import com.streetask.app.model.enums.EventCategory;

public interface EventRepository extends CrudRepository<Event, UUID> {
	Iterable<Event> findByActive(Boolean active);
	Iterable<Event> findByCategory(EventCategory category);
	Iterable<Event> findByActiveAndCategory(Boolean active, EventCategory category);
	Iterable<Event> findByCreatorId(UUID creatorId);
	Iterable<Event> findByCreatorIdAndActive(UUID creatorId, Boolean active);
	long countByCreatorId(UUID creatorId);
}
