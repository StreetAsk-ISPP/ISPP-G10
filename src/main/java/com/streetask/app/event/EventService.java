package com.streetask.app.event;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.Event;
import com.streetask.app.model.EventAttendance;
import com.streetask.app.model.enums.EventCategory;

@Service
public class EventService {

	private final EventRepository eventRepository;
	private final EventAttendanceRepository attendanceRepository;

	public EventService(EventRepository eventRepository, EventAttendanceRepository attendanceRepository) {
		this.eventRepository = eventRepository;
		this.attendanceRepository = attendanceRepository;
	}

	@Transactional(readOnly = true)
	public Event findEvent(UUID id) {
		return eventRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
	}

	@Transactional(readOnly = true)
	public EventDetailDTO getEventDetails(UUID eventId) {
		Event event = findEvent(eventId);
		return convertToDetailDTO(event);
	}

	@Transactional(readOnly = true)
	public EventSummaryDTO getEventSummary(UUID eventId) {
		Event event = findEvent(eventId);
		return convertToSummaryDTO(event);
	}

	@Transactional(readOnly = true)
	public Iterable<Event> findAll() {
		return eventRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Iterable<EventSummaryDTO> findAllSummaries() {
		return ((List<Event>) eventRepository.findAll()).stream()
				.map(this::convertToSummaryDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Iterable<Event> findByActive(Boolean active) {
		return eventRepository.findByActive(active);
	}

	@Transactional(readOnly = true)
	public Iterable<EventSummaryDTO> findActiveSummaries() {
		return ((List<Event>) eventRepository.findByActive(true)).stream()
				.map(this::convertToSummaryDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Iterable<Event> findByCategory(EventCategory category) {
		return eventRepository.findByCategory(category);
	}

	@Transactional(readOnly = true)
	public Iterable<EventSummaryDTO> findByCategorySummaries(EventCategory category) {
		return ((List<Event>) eventRepository.findByCategory(category)).stream()
				.map(this::convertToSummaryDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Iterable<Event> findByActiveAndCategory(Boolean active, EventCategory category) {
		return eventRepository.findByActiveAndCategory(active, category);
	}

	@Transactional(readOnly = true)
	public Iterable<EventSummaryDTO> findByActiveAndCategorySummaries(Boolean active, EventCategory category) {
		return ((List<Event>) eventRepository.findByActiveAndCategory(active, category)).stream()
				.map(this::convertToSummaryDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Iterable<Event> findByCreator(UUID creatorId) {
		return eventRepository.findByCreatorId(creatorId);
	}

	@Transactional(readOnly = true)
	public Iterable<Event> findByCreatorAndActive(UUID creatorId, Boolean active) {
		return eventRepository.findByCreatorIdAndActive(creatorId, active);
	}

	@Transactional(readOnly = true)
	public List<AttendeeDTO> getEventAttendees(UUID eventId) {
		Event event = findEvent(eventId);
		List<EventAttendance> attendances = attendanceRepository.findByEventIdAndIsAttendingTrue(eventId);
		return attendances.stream()
				.map(this::convertToAttendeeDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public long getConfirmedAttendeeCount(UUID eventId) {
		return attendanceRepository.countByEventIdAndIsAttendingTrue(eventId);
	}

	private EventDetailDTO convertToDetailDTO(Event event) {
		List<AttendeeDTO> attendees = attendanceRepository.findByEventIdAndIsAttendingTrue(event.getId())
				.stream()
				.map(this::convertToAttendeeDTO)
				.collect(Collectors.toList());

		long confirmedCount = attendanceRepository.countByEventIdAndIsAttendingTrue(event.getId());

		return EventDetailDTO.builder()
				.id(event.getId())
				.title(event.getTitle())
				.description(event.getDescription())
				.category(event.getCategory())
				.address(event.getAddress())
				.latitude(event.getLocation() != null ? event.getLocation().getLatitude() : null)
				.longitude(event.getLocation() != null ? event.getLocation().getLongitude() : null)
				.startsAt(event.getStartsAt())
				.endsAt(event.getEndsAt())
				.totalAttendeeCount(event.getAttendeeCount())
				.confirmedAttendeeCount((int) confirmedCount)
				.active(event.getActive())
				.featured(event.getFeatured())
				.createdAt(event.getCreatedAt())
				.updatedAt(event.getUpdatedAt())
				.creatorName(event.getCreator() != null ? event.getCreator().getCompanyName() : "Unknown")
				.creatorId(event.getCreator() != null ? event.getCreator().getId() : null)
				.attendees(attendees)
				.build();
	}

	private EventSummaryDTO convertToSummaryDTO(Event event) {
		return EventSummaryDTO.builder()
				.id(event.getId())
				.title(event.getTitle())
				.description(event.getDescription())
				.category(event.getCategory())
				.address(event.getAddress())
				.latitude(event.getLocation() != null ? event.getLocation().getLatitude() : null)
				.longitude(event.getLocation() != null ? event.getLocation().getLongitude() : null)
				.startsAt(event.getStartsAt())
				.endsAt(event.getEndsAt())
				.attendeeCount(event.getAttendeeCount())
				.active(event.getActive())
				.createdAt(event.getCreatedAt())
				.creatorName(event.getCreator() != null ? event.getCreator().getCompanyName() : "Unknown")
				.creatorId(event.getCreator() != null ? event.getCreator().getId() : null)
				.build();
	}

	private AttendeeDTO convertToAttendeeDTO(EventAttendance attendance) {
		return AttendeeDTO.builder()
				.userId(attendance.getRegularUser().getId())
				.userName(attendance.getRegularUser().getUserName())
				.email(attendance.getRegularUser().getEmail())
				.isAttending(attendance.getIsAttending())
				.confirmedAt(attendance.getConfirmedAt())
				.build();
	}
}
