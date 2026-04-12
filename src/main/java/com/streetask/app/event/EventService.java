package com.streetask.app.event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.exceptions.ResourceNotOwnedException;
import com.streetask.app.model.Event;
import com.streetask.app.model.EventAttendance;
import com.streetask.app.model.enums.EventCategory;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;




@Service
public class EventService {
	private final EventRepository eventRepository;
	private final EventAttendanceRepository attendanceRepository;
	private final UserRepository userRepository;

	@Autowired
	public EventService(EventRepository eventRepository, EventAttendanceRepository attendanceRepository,
			UserRepository userRepository) {
		this.eventRepository = eventRepository;
		this.attendanceRepository = attendanceRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public Event findEvent(UUID id) {
		Event event = eventRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
		deactivateIfExpired(event, LocalDateTime.now());
		return event;
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

	@Transactional
	public Iterable<Event> findAll() {
		Iterable<Event> events = eventRepository.findAll();
		LocalDateTime now = LocalDateTime.now();
		for (Event event : events) {
			deactivateIfExpired(event, now);
		}
		return events;
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

	@Scheduled(cron = "0 * * * * *")
	@Transactional
	public void deactivateExpiredEvents() {
		LocalDateTime now = LocalDateTime.now();
		List<Event> expiredActiveEvents = eventRepository.findByActiveTrueAndEndsAtLessThanEqual(now);
		for (Event event : expiredActiveEvents) {
			event.setActive(false);
			event.setUpdatedAt(now);
		}
		if (!expiredActiveEvents.isEmpty()) {
			eventRepository.saveAll(expiredActiveEvents);
		}
	}

	@Transactional
	public Event saveEvent(@Valid Event event) {
		BusinessAccount creator = getAuthenticatedBusinessUser();
		event.setCreator(creator);
		applyDefaultsOnCreate(event);
		eventRepository.save(event);
		return event;
	}

	@Transactional
	public Event updateEvent(@Valid Event event, UUID idToUpdate) {
		Event toUpdate = findEvent(idToUpdate);
		BusinessAccount authenticatedBusiness = getAuthenticatedBusinessUser();

		if (!authenticatedBusiness.getId().equals(toUpdate.getCreator().getId())) {
			throw new ResourceNotOwnedException(toUpdate);
		}

		BeanUtils.copyProperties(event, toUpdate, "id", "creator", "createdAt", "updatedAt", "attendeeCount",
				"questions", "attendances");
		applyDefaultsOnUpdate(toUpdate);
		eventRepository.save(toUpdate);
		return toUpdate;
	}

	@Transactional
	public void deleteEvent(UUID id) {
		Event toDelete = findEvent(id);
		BusinessAccount authenticatedBusiness = getAuthenticatedBusinessUser();

		if (!authenticatedBusiness.getId().equals(toDelete.getCreator().getId())) {
			throw new ResourceNotOwnedException(toDelete);
		}

		eventRepository.delete(toDelete);
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

	private BusinessAccount getAuthenticatedBusinessUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
			throw new AccessDeniedException("Authenticated business user required");
		}

		User user = userRepository.findByEmail(auth.getName().trim())
				.orElseThrow(() -> new AccessDeniedException("Authenticated business user required"));

		if (!(user instanceof BusinessAccount businessAccount)) {
			throw new AccessDeniedException("Only business users can manage events");
		}

		return businessAccount;
	}

	private void applyDefaultsOnCreate(Event event) {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		event.setCreatedAt(now);
		event.setUpdatedAt(now);

		if (event.getActive() == null) {
			event.setActive(true);
		}
		if (event.getFeatured() == null) {
			event.setFeatured(false);
		}
		if (event.getAttendeeCount() == null) {
			event.setAttendeeCount(0);
		}
	}

	private void applyDefaultsOnUpdate(Event event) {
		event.setUpdatedAt(LocalDateTime.now(ZoneId.of("UTC")));
		if (event.getFeatured() == null) {
			event.setFeatured(false);
		}
		if (event.getActive() == null) {
			event.setActive(true);
		}
		if (event.getAttendeeCount() == null) {
			event.setAttendeeCount(0);
		}
	}

	private void deactivateIfExpired(Event event, LocalDateTime now) {
		if (event != null
				&& Boolean.TRUE.equals(event.getActive())
				&& event.getEndsAt() != null
				&& !event.getEndsAt().isAfter(now)) {
			event.setActive(false);
			event.setUpdatedAt(now);
			eventRepository.save(event);
		}
	}
}
