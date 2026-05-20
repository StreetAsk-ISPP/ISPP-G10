package com.streetask.app.event;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import com.streetask.app.model.Event;
import com.streetask.app.model.enums.EventCategory;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends CrudRepository<Event, UUID> {
    Iterable<Event> findByActive(Boolean active);

    Iterable<Event> findByCategory(EventCategory category);

    Iterable<Event> findByActiveAndCategory(Boolean active, EventCategory category);

    Iterable<Event> findByCreatorId(UUID creatorId);

    Iterable<Event> findByCreatorIdAndActive(UUID creatorId, Boolean active);

    List<Event> findByActiveTrueAndEndsAtLessThanEqual(LocalDateTime now);

    long countByCreatorId(UUID creatorId);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, UUID id);
}
