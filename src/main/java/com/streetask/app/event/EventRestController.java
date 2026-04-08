package com.streetask.app.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.streetask.app.model.enums.EventCategory;
import com.streetask.app.util.RestPreconditions;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/events")
@SecurityRequirement(name = "bearerAuth")
public class EventRestController {

	private final EventService eventService;

	public EventRestController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping
	public ResponseEntity<Iterable<EventSummaryDTO>> findAll(@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) EventCategory category) {
		Iterable<EventSummaryDTO> res;
		if (active != null && category != null) {
			res = eventService.findByActiveAndCategorySummaries(active, category);
		} else if (active != null) {
			res = ((List<com.streetask.app.model.Event>) eventService.findByActive(active)).stream()
					.map(event -> eventService.getEventSummary(event.getId()))
					.collect(java.util.stream.Collectors.toList());
		} else if (category != null) {
			res = eventService.findByCategorySummaries(category);
		} else {
			res = eventService.findAllSummaries();
		}
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@GetMapping("/{id}/details")
	public ResponseEntity<EventDetailDTO> getEventDetails(@PathVariable("id") UUID id) {
		RestPreconditions.checkNotNull(eventService.findEvent(id), "Event", "ID", id);
		return new ResponseEntity<>(eventService.getEventDetails(id), HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<EventSummaryDTO> getEventSummary(@PathVariable("id") UUID id) {
		RestPreconditions.checkNotNull(eventService.findEvent(id), "Event", "ID", id);
		return new ResponseEntity<>(eventService.getEventSummary(id), HttpStatus.OK);
	}

	@GetMapping("/{id}/attendees")
	public ResponseEntity<List<AttendeeDTO>> getEventAttendees(@PathVariable("id") UUID id) {
		RestPreconditions.checkNotNull(eventService.findEvent(id), "Event", "ID", id);
		return new ResponseEntity<>(eventService.getEventAttendees(id), HttpStatus.OK);
	}

	@GetMapping("/{id}/attendees/count")
	public ResponseEntity<Map<String, Long>> getAttendeeCount(@PathVariable("id") UUID id) {
		RestPreconditions.checkNotNull(eventService.findEvent(id), "Event", "ID", id);
		long count = eventService.getConfirmedAttendeeCount(id);
		Map<String, Long> response = new HashMap<>();
		response.put("count", count);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
