package com.streetask.app.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.Event;
import com.streetask.app.model.EventAttendance;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.Question;
import com.streetask.app.model.enums.EventCategory;
import com.streetask.app.question.QuestionService;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.UserRepository;

import jakarta.transaction.Transactional;

@WebMvcTest(EventRestController.class)
@DisplayName("EventRestController Unit Tests")
class EventRestControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private QuestionService questionService;

    private UUID eventId;
    private UUID businessAccountId;
    private Event testEvent;
    private BusinessAccount businessAccount;

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
    }

    @Test
    @DisplayName("GET /api/v1/events should return all events")
    @WithMockUser(username = BUSINESS_EMAIL)
    void getAllEvents_shouldReturnOkWithAllEvents() throws Exception {
        when(eventService.findAll()).thenReturn(List.of(testEvent));

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
    @DisplayName("GET /api/v1/events/{id} should return event")
    @WithMockUser(username = BUSINESS_EMAIL)
    void getEventById_shouldReturnOkWithEvent() throws Exception {
        when(eventService.findEvent(eventId)).thenReturn(testEvent);

        mockMvc.perform(get(EVENT_URL + "/{id}", eventId)
                .contentType(APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventId.toString())))
                .andExpect(jsonPath("$.title", is("Summer Festival")))
                .andExpect(jsonPath("$.category", is("LEISURE")));
    }

    @Test
    @DisplayName("GET /api/v1/events/{id} should return not found for non-existent event")
    @WithMockUser(username = BUSINESS_EMAIL)
    void getEventById_shouldReturnNotFoundForNonExistentEvent() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(eventService.findEvent(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Event", "id", nonExistentId));

        mockMvc.perform(get(EVENT_URL + "/{id}", nonExistentId)
                .contentType(APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/events/{id}/attendees should return list of attendees")
    @WithMockUser(username = BUSINESS_EMAIL)
    void getEventAttendees_shouldReturnOkWithAttendees() throws Exception {
        when(eventService.findAttendees(eventId)).thenReturn(List.of());

        mockMvc.perform(get(EVENT_URL + "/{id}/attendees", eventId)
                .contentType(APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/events/{id}/questions should map creator and null creator branches")
    @WithMockUser(username = BUSINESS_EMAIL)
    void getEventQuestions_shouldReturnMappedPayload() throws Exception {
        Question withCreator = new Question();
        withCreator.setId(UUID.randomUUID());
        withCreator.setTitle("Question title");
        withCreator.setContent("Question content");
        withCreator.setCreatedAt(Instant.now());

        RegularUser creator = new RegularUser();
        creator.setId(UUID.randomUUID());
        creator.setUserName("john-user");
        withCreator.setCreator(creator);

        Question withoutCreator = new Question();
        withoutCreator.setId(UUID.randomUUID());
        withoutCreator.setTitle("Question without creator");
        withoutCreator.setContent("Content no creator");
        withoutCreator.setCreatedAt(Instant.now());
        withoutCreator.setCreator(null);

        when(questionService.findByEvent(eventId)).thenReturn(List.of(withCreator, withoutCreator));

        mockMvc.perform(get(EVENT_URL + "/{id}/questions", eventId)
                .contentType(APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(withCreator.getId().toString())))
                .andExpect(jsonPath("$[0].creatorId", is(creator.getId().toString())))
                .andExpect(jsonPath("$[0].creatorUserName", is("john-user")))
                .andExpect(jsonPath("$[1].id", is(withoutCreator.getId().toString())))
                .andExpect(jsonPath("$[1].creatorId").doesNotExist())
                .andExpect(jsonPath("$[1].creatorUserName").doesNotExist());
    }
}

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventAttendanceRepository eventAttendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Event event1;
    private BusinessAccount creator;
    private BusinessAccount otherCreator;

    @BeforeEach
    void setUp() {
        Authorities businessAuthority = entityManager.find(Authorities.class,
                java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"));

        creator = new BusinessAccount();
        creator.setEmail("eventcreator@streetask.com");
        creator.setUserName("eventcreator");
        creator.setFirstName("Event");
        creator.setLastName("Creator");
        creator.setCompanyName("Events Corp");
        creator.setTaxId("B11223344");
        creator.setAuthority(businessAuthority);
        creator.setVerified(true);
        creator = (BusinessAccount) userRepository.save(creator);

        event1 = new Event();
        event1.setTitle("Event Endpoint Test 1");
        event1.setDescription("First event for endpoint tests");
        event1.setCreator(creator);
        event1 = eventRepository.save(event1);

        Event event2 = new Event();
        event2.setTitle("Event Endpoint Test 2");
        event2.setDescription("Second event for endpoint tests");
        event2.setCreator(creator);
        eventRepository.save(event2);

        otherCreator = new BusinessAccount();
        otherCreator.setEmail("othercreator@streetask.com");
        otherCreator.setUserName("othercreator");
        otherCreator.setFirstName("Other");
        otherCreator.setLastName("Creator");
        otherCreator.setCompanyName("Other Events Corp");
        otherCreator.setTaxId("B44332211");
        otherCreator.setAuthority(businessAuthority);
        otherCreator.setVerified(true);
        otherCreator = (BusinessAccount) userRepository.save(otherCreator);

        Authorities regularAuthority = entityManager.find(Authorities.class,
                java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"));
        RegularUser regularUser = new RegularUser();
        regularUser.setEmail("regular@streetask.com");
        regularUser.setUserName("regularuser");
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setVisibilityRadiusKm(5.0f);
        regularUser.setPremiumActive(false);
        regularUser.setAuthority(regularAuthority);
        userRepository.save(regularUser);
    }

    @Test
    @WithMockUser
    void findAll_shouldReturnAllEvents() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Event Endpoint Test 1")))
                .andExpect(jsonPath("$[*].title", hasItem("Event Endpoint Test 2")));
    }

    @Test
    void findAll_shouldReturnUnauthorizedWhenUserIsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void findById_shouldReturnEventWhenExists() throws Exception {
        mockMvc.perform(get("/api/v1/events/{id}", event1.getId())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event1.getId().toString()))
                .andExpect(jsonPath("$.title").value("Event Endpoint Test 1"))
                .andExpect(jsonPath("$.description").value("First event for endpoint tests"));
    }

    @Test
    @WithMockUser
    void findById_shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/events/{id}", nonExistentId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void findById_shouldDeactivateEventWhenEndDateIsReached() throws Exception {
        Event expiredEvent = new Event();
        expiredEvent.setTitle("Expired Event");
        expiredEvent.setDescription("Should become inactive automatically");
        expiredEvent.setCreator(creator);
        expiredEvent.setActive(true);
        expiredEvent.setEndsAt(LocalDateTime.now().minusMinutes(1));
        expiredEvent = eventRepository.save(expiredEvent);

        mockMvc.perform(get("/api/v1/events/{id}", expiredEvent.getId())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Event persisted = eventRepository.findById(expiredEvent.getId()).orElseThrow();
        assertThat(persisted.getActive()).isFalse();
    }

    @Test
    @WithMockUser(username = "eventcreator@streetask.com")
    void create_shouldCreateEventWhenPayloadIsValid() throws Exception {
        Map<String, Object> payload = createValidEventPayload();

        mockMvc.perform(post("/api/v1/events")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Backend Created Event"))
                .andExpect(jsonPath("$.description").value("Event created from REST test"))
                .andExpect(jsonPath("$.creator.id").value(creator.getId().toString()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.featured").value(false))
                .andExpect(jsonPath("$.attendeeCount").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(username = "regular@streetask.com")
    void create_shouldReturnForbiddenWhenAuthenticatedUserIsNotBusiness() throws Exception {
        Map<String, Object> payload = createValidEventPayload();

        mockMvc.perform(post("/api/v1/events")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldReturnUnauthorizedWhenUserIsAnonymous() throws Exception {
        Map<String, Object> payload = createValidEventPayload();

        mockMvc.perform(post("/api/v1/events")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "regular@streetask.com")
    void toggleAttendance_shouldMarkAndUnmarkAttendanceForAuthenticatedRegularUser() throws Exception {
        mockMvc.perform(post("/api/v1/events/{eventId}/attendance", event1.getId())
                .with(csrf())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event1.getId().toString()))
                .andExpect(jsonPath("$.myAttendance").value(true))
                .andExpect(jsonPath("$.attendeeCount").value(1));

        mockMvc.perform(post("/api/v1/events/{eventId}/attendance", event1.getId())
                .with(csrf())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event1.getId().toString()))
                .andExpect(jsonPath("$.myAttendance").value(false))
                .andExpect(jsonPath("$.attendeeCount").value(0));
    }

    @Test
    @WithMockUser(username = "eventcreator@streetask.com")
    void findAttendees_shouldReturnAttendingUsersForOwnedEvent() throws Exception {
        RegularUser attendee = (RegularUser) userRepository.findByEmail("regular@streetask.com").orElseThrow();

        EventAttendance attendance = new EventAttendance();
        attendance.setRegularUser(attendee);
        attendance.setEvent(event1);
        attendance.setIsAttending(true);
        attendance.setConfirmedAt(LocalDateTime.now());
        eventAttendanceRepository.save(attendance);

        mockMvc.perform(get("/api/v1/events/{eventId}/attendees", event1.getId())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(attendee.getId().toString()))
                .andExpect(jsonPath("$[0].userName").value(attendee.getUserName()))
                .andExpect(jsonPath("$[0].firstName").value(attendee.getFirstName()))
                .andExpect(jsonPath("$[0].lastName").value(attendee.getLastName()));
    }

    @Test
    @WithMockUser(username = "eventcreator@streetask.com")
    void update_shouldUpdateEventWhenAuthenticatedBusinessOwnsEvent() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Updated Event Title");
        payload.put("description", "Updated Event Description");
        payload.put("active", false);

        mockMvc.perform(put("/api/v1/events/{eventId}", event1.getId())
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event1.getId().toString()))
                .andExpect(jsonPath("$.title").value("Updated Event Title"))
                .andExpect(jsonPath("$.description").value("Updated Event Description"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(username = "othercreator@streetask.com")
    void update_shouldReturnBadRequestWhenAuthenticatedBusinessDoesNotOwnEvent() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Unauthorized Update");
        payload.put("description", "This update should fail");

        mockMvc.perform(put("/api/v1/events/{eventId}", event1.getId())
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "eventcreator@streetask.com")
    void delete_shouldDeleteEventWhenAuthenticatedBusinessOwnsEvent() throws Exception {
        RegularUser attendee = (RegularUser) userRepository.findByEmail("regular@streetask.com").orElseThrow();
        EventAttendance attendance = new EventAttendance();
        attendance.setRegularUser(attendee);
        attendance.setEvent(event1);
        attendance.setIsAttending(true);
        attendance.setConfirmedAt(LocalDateTime.now());
        eventAttendanceRepository.save(attendance);

        mockMvc.perform(delete("/api/v1/events/{eventId}", event1.getId())
                .with(csrf())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event deleted!"));

        assertThat(eventRepository.findById(event1.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "othercreator@streetask.com")
    void delete_shouldReturnBadRequestWhenAuthenticatedBusinessDoesNotOwnEvent() throws Exception {
        mockMvc.perform(delete("/api/v1/events/{eventId}", event1.getId())
                .with(csrf())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private Map<String, Object> createValidEventPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Backend Created Event");
        payload.put("description", "Event created from REST test");
        payload.put("address", "Calle Falsa 123");
        payload.put("category", "OTHER");
        return payload;
    }
}