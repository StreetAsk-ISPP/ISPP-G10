package com.streetask.app.event;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.streetask.app.model.Event;

public interface EventRepository extends CrudRepository<Event, UUID> {
}
