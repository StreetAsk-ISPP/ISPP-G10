package com.streetask.app.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.Event;
import com.streetask.app.model.EventAttendance;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.enums.EventCategory;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.RegularUser;

@SpringBootTest
@DisplayName("EventService Unit Tests")
class EventServiceTest {

	@Autowired
	private EventService eventService;

	@MockitoBean
	private EventRepository eventRepository;

	@MockitoBean
	private EventAttendanceRepository attendanceRepository;

	private UUID eventId;
	private UUID businessAccountId;
	private UUID regularUserId;
	private Event testEvent;
	private BusinessAccount businessAccount;
	private RegularUser regularUser;
	private EventAttendance attendance;

	private static final String BUSINESS_EMAIL = "business@test.com";
	private static final String BUSINESS_NAME = "Event Organizers Inc";
	private static final String USER_EMAIL = "user@test.com";
	private static final String USER_NAME = "johndoe";

	@BeforeEach
	void setUp() {
		eventId = UUID.randomUUID();
		businessAccountId = UUID.randomUUID();
		regularUserId = UUID.randomUUID();

		// Setup business account
		businessAccount = new BusinessAccount();
		businessAccount.setId(businessAccountId);
		businessAccount.setEmail(BUSINESS_EMAIL);
		businessAccount.setCompanyName(BUSINESS_NAME);
		businessAccount.setUserName("business_user");
		Authorities businessAuthority = new Authorities();
		businessAuthority.setAuthority("BUSINESS");
		businessAccount.setAuthority(businessAuthority);

		// Setup regular user
		regularUser = new RegularUser();
		regularUser.setId(regularUserId);
		regularUser.setEmail(USER_EMAIL);
		regularUser.setUserName(USER_NAME);
		regularUser.setFirstName("John");
		regularUser.setLastName("Doe");
		Authorities userAuthority = new Authorities();
		userAuthority.setAuthority("USER");
		regularUser.setAuthority(userAuthority);

		// Setup event
		testEvent = new Event();
		testEvent.setId(eventId);
		testEvent.setTitle("Tech Conference 2026");
		testEvent.setDescription("Annual technology conference with industry leaders");
		testEvent.setCategory(EventCategory.CULTURE);
		testEvent.setCreator(businessAccount);

		GeoPoint location = new GeoPoint();
		location.setLatitude(40.7128);
		location.setLongitude(-74.0060);
		testEvent.setLocation(location);

		testEvent.setAddress("New York Convention Center");
		testEvent.setStartsAt(LocalDateTime.of(2026, 5, 15, 9, 0));
		testEvent.setEndsAt(LocalDateTime.of(2026, 5, 15, 17, 0));
		testEvent.setAttendeeCount(500);
		testEvent.setActive(true);
		testEvent.setFeatured(true);
		testEvent.setCreatedAt(LocalDateTime.now());
		testEvent.setUpdatedAt(LocalDateTime.now());

		// Setup attendance
		attendance = new EventAttendance();
		attendance.setId(UUID.randomUUID());
		attendance.setEvent(testEvent);
		attendance.setRegularUser(regularUser);
		attendance.setIsAttending(true);
		attendance.setConfirmedAt(LocalDateTime.now());
	}

	@Test
	@DisplayName("getEventDetails should return EventDetailDTO with attendees")
	void getEventDetails_shouldReturnEventDetailDTOWithAttendees() {
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
		when(attendanceRepository.countByEventIdAndIsAttendingTrue(eventId)).thenReturn(125L);
		when(attendanceRepository.findByEventIdAndIsAttendingTrue(eventId))
				.thenReturn(Arrays.asList(attendance));

		EventDetailDTO result = eventService.getEventDetails(eventId);

		assertNotNull(result);
		assertEquals(eventId, result.getId());
		assertEquals("Tech Conference 2026", result.getTitle());
		assertEquals(EventCategory.CULTURE, result.getCategory());
		assertEquals(125, result.getConfirmedAttendeeCount());
		assertEquals(500, result.getTotalAttendeeCount());
		assertTrue(result.getFeatured());
		assertEquals(1, result.getAttendees().size());
		assertEquals(USER_NAME, result.getAttendees().get(0).getUserName());
	}

	@Test
	@DisplayName("getEventDetails should throw ResourceNotFoundException for non-existent event")
	void getEventDetails_shouldThrowNotFoundForMissingEvent() {
		when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> {
			eventService.getEventDetails(eventId);
		});
	}

	@Test
	@DisplayName("getEventSummary should return EventSummaryDTO without attendees list")
	void getEventSummary_shouldReturnEventSummaryDTO() {
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

		EventSummaryDTO result = eventService.getEventSummary(eventId);

		assertNotNull(result);
		assertEquals(eventId, result.getId());
		assertEquals("Tech Conference 2026", result.getTitle());
		assertEquals("Annual technology conference with industry leaders", result.getDescription());
		assertEquals(EventCategory.CULTURE, result.getCategory());
		assertEquals("New York Convention Center", result.getAddress());
		assertEquals(40.7128, result.getLatitude());
		assertEquals(-74.0060, result.getLongitude());
		assertEquals(BUSINESS_NAME, result.getCreatorName());
		assertEquals(businessAccountId, result.getCreatorId());
	}

	@Test
	@DisplayName("getConfirmedAttendeeCount should return correct count of attending users")
	void getConfirmedAttendeeCount_shouldReturnCorrectCount() {
		when(attendanceRepository.countByEventIdAndIsAttendingTrue(eventId)).thenReturn(42L);

		long count = eventService.getConfirmedAttendeeCount(eventId);

		assertEquals(42L, (long) count);
	}

	@Test
	@DisplayName("getEventAttendees should return list of AttendeeDTO objects")
	void getEventAttendees_shouldReturnListOfAttendeeDTOs() {
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
		when(attendanceRepository.findByEventIdAndIsAttendingTrue(eventId))
				.thenReturn(Arrays.asList(attendance));

		List<AttendeeDTO> result = eventService.getEventAttendees(eventId);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(regularUserId, result.get(0).getUserId());
		assertEquals(USER_NAME, result.get(0).getUserName());
		assertEquals(USER_EMAIL, result.get(0).getEmail());
		assertTrue(result.get(0).getIsAttending());
	}

	@Test
	@DisplayName("getEventAttendees should return empty list when no attendees")
	void getEventAttendees_shouldReturnEmptyListWhenNoAttendees() {
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
		when(attendanceRepository.findByEventIdAndIsAttendingTrue(eventId))
				.thenReturn(Arrays.asList());

		List<AttendeeDTO> result = eventService.getEventAttendees(eventId);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}
}
