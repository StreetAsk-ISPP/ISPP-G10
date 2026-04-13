package com.streetask.app.event;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.model.Event;
import com.streetask.app.util.RestPreconditions;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/events")
@SecurityRequirement(name = "bearerAuth")
public class EventRestController {

    private final EventService eventService;

    @Autowired
    public EventRestController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<Iterable<Event>> findAll() {
        return new ResponseEntity<>(eventService.findAll(), HttpStatus.OK);
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<Event> findById(@PathVariable("id") UUID id) {
        return new ResponseEntity<>(eventService.findEvent(id), HttpStatus.OK);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Event> create(@RequestBody @Valid Event event) {
        Event savedEvent = eventService.saveEvent(event);
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }

    @PostMapping(value = "{eventId}/attendance")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Event> toggleAttendance(@PathVariable("eventId") UUID id) {
        return new ResponseEntity<>(eventService.toggleAttendance(id), HttpStatus.OK);
    }

    @GetMapping(value = "{eventId}/attendees")
    public ResponseEntity<java.util.List<EventAttendeeSummary>> findAttendees(@PathVariable("eventId") UUID id) {
        return new ResponseEntity<>(eventService.findAttendees(id), HttpStatus.OK);
    }

    @PutMapping(value = "{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Event> update(@PathVariable("eventId") UUID id,
            @RequestBody @Valid Event event) {
        RestPreconditions.checkNotNull(eventService.findEvent(id), "Event", "ID", id);
        return new ResponseEntity<>(this.eventService.updateEvent(event, id), HttpStatus.OK);
    }

    @DeleteMapping(value = "{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<MessageResponse> delete(@PathVariable("eventId") UUID id) {
        RestPreconditions.checkNotNull(eventService.findEvent(id), "Event", "ID", id);
        eventService.deleteEvent(id);
        return new ResponseEntity<>(new MessageResponse("Event deleted!"), HttpStatus.OK);
    }
}