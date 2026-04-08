package com.streetask.app.event;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.Event;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.enums.EventCategory;
import com.streetask.app.user.Authorities;

@WebMvcTest(EventRestController.class)
@DisplayName("EventRestController Unit Tests")
class EventRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EventService eventService;

	private UUID eventId;
	private UUID businessAccountId;
	private Event testEvent;
	private BusinessAccount businessAccount;
	private EventDetailDTO testEventDetail;
	private EventSummaryDTO testEventSummary;

	private static final String BUSINESS_EMAIL = "business@test.com";
	private static final String BUSINESS_NAME = "Test Business";
	private static final String EVENT_URL = "/api/v1/events";

	@BeforeEach
	void setUp() {
		eventId = UUID.randomUUID();
		businessAccountId = UUID.randomUUID();

		businessAccount = new BusinessAccount();
		businessAccount.setId(businessAccountId);
		businessAccount.setEmail(BUSINESS_EMAIL);
		businessAccount.setCompanyName(BUSINESS_NAME);
		Authorities authority = new Authorities();
		authority.setAuthority("BUSINESS");
		businessAccount.setAuthority(authority);

		testEvent = new Event();
		testEvent.setId(eventId);
		testEvent.setTitle("Summer Festival");
		testEvent.setDescription("Annual summer community festival");
		testEvent.setCategory(EventCategory.LEISURE);
		testEvent.setCreator(businessAccount);

		GeoPoint location = new GeoPoint();
		location.setLatitude(40.7128);
		location.setLongitude(-74.0060);
		testEvent.setLocation(location);

		testEvent.setAddress("Central Park, New York");
		testEvent.setStartsAt(LocalDateTime.of(2026, 6, 15, 10, 0));
		testEvent.setEndsAt(LocalDateTime.of(2026, 6, 15, 18, 0));
		testEvent.setAttendeeCount(150);
		testEvent.setActive(true);
		testEvent.setFeatured(false);
		testEvent.setCreatedAt(LocalDateTime.now());
		testEvent.setUpdatedAt(LocalDateTime.now());

		testEventSummary = EventSummaryDTO.builder()
				.id(eventId)
				.title(testEvent.getTitle())
				.description(testEvent.getDescription())
				.category(testEvent.getCategory())
				.address(testEvent.getAddress())
				.latitude(location.getLatitude())
				.longitude(location.getLongitude())
				.startsAt(testEvent.getStartsAt())
				.endsAt(testEvent.getEndsAt())
				.attendeeCount(testEvent.getAttendeeCount())
				.active(testEvent.getActive())
				.createdAt(testEvent.getCreatedAt())
				.creatorName(BUSINESS_NAME)
				.creatorId(businessAccountId)
				.build();

		testEventDetail = EventDetailDTO.builder()
				.id(eventId)
				.title(testEvent.getTitle())
				.description(testEvent.getDescription())
				.category(testEvent.getCategory())
				.address(testEvent.getAddress())
				.latitude(location.getLatitude())
				.longitude(location.getLongitude())
				.startsAt(testEvent.getStartsAt())
				.endsAt(testEvent.getEndsAt())
				.totalAttendeeCount(testEvent.getAttendeeCount())
				.confirmedAttendeeCount(120)
				.active(testEvent.getActive())
				.featured(testEvent.getFeatured())
				.createdAt(testEvent.getCreatedAt())
				.updatedAt(testEvent.getUpdatedAt())
				.creatorName(BUSINESS_NAME)
				.creatorId(businessAccountId)
				.attendees(Arrays.asList())
				.build();
	}

	@Test
	@DisplayName("GET /api/v1/events should return all event summaries")
	@WithMockUser(username = BUSINESS_EMAIL)
	void getAllEvents_shouldReturnOkWithAllEvents() throws Exception {
		when(eventService.findAllSummaries()).thenReturn(Arrays.asList(testEventSummary));

		mockMvc.perform(get(EVENT_URL)
				.contentType(APPLICATION_JSON)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(eventId.toString())))
				.andExpect(jsonPath("$[0].title", is("Summer Festival")))
				.andExpect(jsonPath("$[0].category", is("LEISURE")));
	}

	@Test
	@DisplayName("GET /api/v1/events/{id}/details should return event details")
	@WithMockUser(username = BUSINESS_EMAIL)
	void getEventDetails_shouldReturnOkWithEventDetails() throws Exception {
		when(eventService.findEvent(eventId)).thenReturn(testEvent);
		when(eventService.getEventDetails(eventId)).thenReturn(testEventDetail);

		mockMvc.perform(get(EVENT_URL + "/{id}/details", eventId)
				.contentType(APPLICATION_JSON)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(eventId.toString())))
				.andExpect(jsonPath("$.title", is("Summer Festival")))
				.andExpect(jsonPath("$.category", is("LEISURE")));
	}

	@Test
	@DisplayName("GET /api/v1/events/{id}/details should return not found for non-existent event")
	@WithMockUser(username = BUSINESS_EMAIL)
	void getEventDetails_shouldReturnNotFoundForNonExistentEvent() throws Exception {
		UUID nonExistentId = UUID.randomUUID();
		when(eventService.findEvent(nonExistentId))
				.thenThrow(new ResourceNotFoundException("Event", "id", nonExistentId));

		mockMvc.perform(get(EVENT_URL + "/{id}/details", nonExistentId)
				.contentType(APPLICATION_JSON)
				.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("GET /api/v1/events/{id} should return event summary")
	@WithMockUser(username = BUSINESS_EMAIL)
	void getEventSummary_shouldReturnOkWithEventSummary() throws Exception {
		when(eventService.findEvent(eventId)).thenReturn(testEvent);
		when(eventService.getEventSummary(eventId)).thenReturn(testEventSummary);

		mockMvc.perform(get(EVENT_URL + "/{id}", eventId)
				.contentType(APPLICATION_JSON)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(eventId.toString())))
				.andExpect(jsonPath("$.title", is("Summer Festival")));
	}

	@Test
	@DisplayName("GET /api/v1/events/{id}/attendees should return list of attendees")
	@WithMockUser(username = BUSINESS_EMAIL)
	void getEventAttendees_shouldReturnOkWithAttendees() throws Exception {
		when(eventService.findEvent(eventId)).thenReturn(testEvent);
		when(eventService.getEventAttendees(eventId)).thenReturn(Arrays.asList());

		mockMvc.perform(get(EVENT_URL + "/{id}/attendees", eventId)
				.contentType(APPLICATION_JSON)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("GET /api/v1/events/{id}/attendees/count should return attendee count")
	@WithMockUser(username = BUSINESS_EMAIL)
	void getAttendeeCount_shouldReturnOkWithCount() throws Exception {
		when(eventService.findEvent(eventId)).thenReturn(testEvent);
		when(eventService.getConfirmedAttendeeCount(eventId)).thenReturn(120L);

		mockMvc.perform(get(EVENT_URL + "/{id}/attendees/count", eventId)
				.contentType(APPLICATION_JSON)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count", is(120)));
	}
}
