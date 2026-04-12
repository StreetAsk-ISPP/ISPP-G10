package com.streetask.app.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.streetask.app.model.Event;

public interface EventRepository extends CrudRepository<Event, UUID> {

    List<Event> findByActiveTrueAndEndsAtLessThanEqual(LocalDateTime now);
}
