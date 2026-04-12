package com.streetask.app.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.exceptions.ResourceNotOwnedException;
import com.streetask.app.model.Event;
import com.streetask.app.model.EventAttendance;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.enums.EventCategory;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.UserRepository;

// ===== UNIT TESTS =====
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceUnitTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private EventService eventService;

	private BusinessAccount owner;
	private BusinessAccount otherBusiness;

	@BeforeEach
	void setUp() {
		owner = new BusinessAccount();
		owner.setId(UUID.randomUUID());
		owner.setEmail("owner@streetask.com");

		otherBusiness = new BusinessAccount();
		otherBusiness.setId(UUID.randomUUID());
		otherBusiness.setEmail("other@streetask.com");

		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void saveEvent_shouldAssignCreatorAndDefaultsForAuthenticatedBusiness() {
		authenticateAs(owner.getEmail());
		when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
		when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Event event = new Event();
		event.setTitle("Service Event");
		event.setDescription("Created from service test");

		Event saved = eventService.saveEvent(event);

		assertNotNull(saved.getCreator());
		assertEquals(owner.getId(), saved.getCreator().getId());
		assertTrue(saved.getActive());
		assertFalse(saved.getFeatured());
		assertEquals(0, saved.getAttendeeCount());
		assertNotNull(saved.getCreatedAt());
		assertNotNull(saved.getUpdatedAt());
		verify(eventRepository).save(event);
	}

	@Test
	void saveEvent_shouldThrowAccessDeniedWhenAuthenticatedUserIsNotBusiness() {
		RegularUser regularUser = new RegularUser();
		regularUser.setId(UUID.randomUUID());
		regularUser.setEmail("regular@streetask.com");

		authenticateAs(regularUser.getEmail());
		when(userRepository.findByEmail(regularUser.getEmail())).thenReturn(Optional.of(regularUser));

		Event event = new Event();
		event.setTitle("Invalid Event");
		event.setDescription("Should fail");

		assertThrows(AccessDeniedException.class, () -> eventService.saveEvent(event));
		verify(eventRepository, never()).save(any(Event.class));
	}

	@Test
	void updateEvent_shouldUpdateWhenAuthenticatedBusinessOwnsEvent() {
		UUID eventId = UUID.randomUUID();
		Event existing = new Event();
		existing.setId(eventId);
		existing.setCreator(owner);
		existing.setTitle("Original Title");
		existing.setDescription("Original Description");

		Event incoming = new Event();
		incoming.setTitle("Updated Title");
		incoming.setDescription("Updated Description");
		incoming.setActive(false);

		authenticateAs(owner.getEmail());
		when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));
		when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Event updated = eventService.updateEvent(incoming, eventId);

		assertEquals(eventId, updated.getId());
		assertEquals("Updated Title", updated.getTitle());
		assertEquals("Updated Description", updated.getDescription());
		assertFalse(updated.getActive());
		assertEquals(owner.getId(), updated.getCreator().getId());
		verify(eventRepository).save(existing);
	}

	@Test
	void updateEvent_shouldThrowResourceNotOwnedWhenBusinessDoesNotOwnEvent() {
		UUID eventId = UUID.randomUUID();
		Event existing = new Event();
		existing.setId(eventId);
		existing.setCreator(owner);

		Event incoming = new Event();
		incoming.setTitle("Unauthorized");
		incoming.setDescription("Unauthorized");

		authenticateAs(otherBusiness.getEmail());
		when(userRepository.findByEmail(otherBusiness.getEmail())).thenReturn(Optional.of(otherBusiness));
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));

		assertThrows(ResourceNotOwnedException.class, () -> eventService.updateEvent(incoming, eventId));
		verify(eventRepository, never()).save(any(Event.class));
	}

	@Test
	void deleteEvent_shouldDeleteWhenAuthenticatedBusinessOwnsEvent() {
		UUID eventId = UUID.randomUUID();
		Event existing = new Event();
		existing.setId(eventId);
		existing.setCreator(owner);

		authenticateAs(owner.getEmail());
		when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
		when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));

		eventService.deleteEvent(eventId);

		verify(eventRepository).delete(existing);
	}

	@Test
	void findEvent_shouldDeactivateExpiredActiveEvent() {
		UUID eventId = UUID.randomUUID();
		Event existing = new Event();
		existing.setId(eventId);
		existing.setActive(true);
		existing.setEndsAt(LocalDateTime.now().minusMinutes(1));

		when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));
		when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Event found = eventService.findEvent(eventId);

		assertFalse(found.getActive());
		assertNotNull(found.getUpdatedAt());
		verify(eventRepository).save(existing);
	}

	@Test
	void findEvent_shouldThrowResourceNotFoundWhenEventDoesNotExist() {
		UUID eventId = UUID.randomUUID();
		when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> eventService.findEvent(eventId));
	}

	@Test
	void deactivateExpiredEvents_shouldDeactivateAllExpiredActiveEvents() {
		Event expired1 = new Event();
		expired1.setId(UUID.randomUUID());
		expired1.setActive(true);
		expired1.setEndsAt(LocalDateTime.now().minusHours(1));

		Event expired2 = new Event();
		expired2.setId(UUID.randomUUID());
		expired2.setActive(true);
		expired2.setEndsAt(LocalDateTime.now().minusMinutes(30));

		when(eventRepository.findByActiveTrueAndEndsAtLessThanEqual(any(LocalDateTime.class)))
				.thenReturn(List.of(expired1, expired2));

		eventService.deactivateExpiredEvents();

		assertFalse(expired1.getActive());
		assertFalse(expired2.getActive());
		verify(eventRepository, times(1)).saveAll(any());
	}

	private void authenticateAs(String email) {
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, null,
				List.of());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}

// ===== INTEGRATION TESTS =====
@SpringBootTest
@DisplayName("EventService Integration Tests")
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
