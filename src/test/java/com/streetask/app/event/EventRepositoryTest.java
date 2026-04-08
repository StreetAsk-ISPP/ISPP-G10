package com.streetask.app.event;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.model.Event;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.enums.EventCategory;
import com.streetask.app.user.Authorities;

@DataJpaTest
@DisplayName("EventRepository Integration Tests")
class EventRepositoryTest {

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private TestEntityManager entityManager;

	// Fixed UUID from data.sql — matches the 'BUSINESS' authority seed row
	private static final UUID BUSINESS_AUTHORITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	private UUID businessAccountId;
	private BusinessAccount businessAccount;
	private Event activeCultureEvent;
	private Event activeLectureEvent;
	private Event inactiveEventEvent;
	private Event inactiveGastronomyEvent;

	@BeforeEach
	void setUp() {
		// Reuse the BUSINESS authority already inserted by data.sql — never insert a new one
		Authorities authority = entityManager.find(Authorities.class, BUSINESS_AUTHORITY_ID);

		// Create business account — do NOT set ID manually; capture it after persist
		businessAccount = new BusinessAccount();
		businessAccount.setEmail("organizer@test.com");
		businessAccount.setUserName("organizer");
		businessAccount.setFirstName("Event");
		businessAccount.setLastName("Organizer");
		businessAccount.setCompanyName("Event Company");
		businessAccount.setTaxId("TAX-12345678");   // required: @NotBlank
		businessAccount.setPassword("hashed_password");
		businessAccount.setAuthority(authority);
		businessAccount.setActive(true);
		businessAccount = entityManager.persistAndFlush(businessAccount);
		businessAccountId = businessAccount.getId(); // capture generated ID

		// Create CULTURE event (active)
		activeCultureEvent = createEvent("Culture Fest", EventCategory.CULTURE, true, businessAccount);
		entityManager.persist(activeCultureEvent);

		// Create another CULTURE event (active)
		Event anotherCultureEvent = createEvent("Art Exhibition", EventCategory.CULTURE, true, businessAccount);
		entityManager.persist(anotherCultureEvent);

		// Create LEISURE event (active)
		activeLectureEvent = createEvent("Music Festival", EventCategory.LEISURE, true, businessAccount);
		entityManager.persist(activeLectureEvent);

		// Create CULTURE event (inactive)
		inactiveEventEvent = createEvent("Past Culture Event", EventCategory.CULTURE, false, businessAccount);
		entityManager.persist(inactiveEventEvent);

		// Create GASTRONOMY event (inactive)
		inactiveGastronomyEvent = createEvent("Old Food Festival", EventCategory.GASTRONOMY, false, businessAccount);
		entityManager.persistAndFlush(inactiveGastronomyEvent);
	}

	private Event createEvent(String title, EventCategory category, boolean active, BusinessAccount creator) {
		Event event = new Event();
		// Do NOT set ID manually — let Hibernate generate it
		event.setTitle(title);
		event.setDescription("Description for " + title);
		event.setCategory(category);
		event.setCreator(creator);
		event.setActive(active);
		event.setFeatured(false);
		event.setAttendeeCount(0);
		event.setStartsAt(LocalDateTime.now().plusDays(1));
		event.setEndsAt(LocalDateTime.now().plusDays(1).plusHours(3));

		GeoPoint location = new GeoPoint();
		location.setLatitude(40.7128);
		location.setLongitude(-74.0060);
		event.setLocation(location);
		event.setAddress("Test Location");

		return event;
	}

	@Test
	@DisplayName("findByActive should return only active events")
	void findByActive_shouldReturnOnlyActiveEvents() {
		List<Event> activeEvents = ((List<Event>) eventRepository.findByActive(true));

		assertEquals(3, activeEvents.size());
		assertTrue(activeEvents.stream().allMatch(Event::getActive));
	}

	@Test
	@DisplayName("findByActive should return only inactive events")
	void findByActive_shouldReturnOnlyInactiveEvents() {
		List<Event> inactiveEvents = ((List<Event>) eventRepository.findByActive(false));

		assertEquals(2, inactiveEvents.size());
		assertTrue(inactiveEvents.stream().allMatch(event -> !event.getActive()));
	}

	@Test
	@DisplayName("findByCategory should return events of specified category")
	void findByCategory_shouldReturnEventsOfCategory() {
		List<Event> cultureEvents = ((List<Event>) eventRepository.findByCategory(EventCategory.CULTURE));

		assertEquals(3, cultureEvents.size());
		assertTrue(cultureEvents.stream().allMatch(event -> event.getCategory() == EventCategory.CULTURE));
	}

	@Test
	@DisplayName("findByCategory should return empty list for category with no events")
	void findByCategory_shouldReturnEmptyForUnusedCategory() {
		List<Event> emergencyEvents = ((List<Event>) eventRepository.findByCategory(EventCategory.EMERGENCY));

		assertTrue(emergencyEvents.isEmpty());
	}

	@Test
	@DisplayName("findByActiveAndCategory should return active events of specified category")
	void findByActiveAndCategory_shouldReturnOnlyActiveEventsOfCategory() {
		List<Event> activeLeisureEvents = ((List<Event>) eventRepository.findByActiveAndCategory(true, EventCategory.LEISURE));

		assertEquals(1, activeLeisureEvents.size());
		assertEquals("Music Festival", activeLeisureEvents.get(0).getTitle());
		assertTrue(activeLeisureEvents.get(0).getActive());
		assertEquals(EventCategory.LEISURE, activeLeisureEvents.get(0).getCategory());
	}

	@Test
	@DisplayName("findByActiveAndCategory should return inactive events of specified category")
	void findByActiveAndCategory_shouldReturnInactiveEventsOfCategory() {
		List<Event> inactiveGastronomyEvents = ((List<Event>) eventRepository.findByActiveAndCategory(false, EventCategory.GASTRONOMY));

		assertEquals(1, inactiveGastronomyEvents.size());
		assertEquals("Old Food Festival", inactiveGastronomyEvents.get(0).getTitle());
		assertFalse(inactiveGastronomyEvents.get(0).getActive());
		assertEquals(EventCategory.GASTRONOMY, inactiveGastronomyEvents.get(0).getCategory());
	}

	@Test
	@DisplayName("findByCreatorId should return only events created by specific business account")
	void findByCreatorId_shouldReturnEventsByCreator() {
		List<Event> businessEvents = ((List<Event>) eventRepository.findByCreatorId(businessAccountId));

		assertEquals(5, businessEvents.size());
		assertTrue(businessEvents.stream().allMatch(event -> event.getCreator().getId().equals(businessAccountId)));
	}

	@Test
	@DisplayName("findByCreatorId should return empty list for business account with no events")
	void findByCreatorId_shouldReturnEmptyForBusinessWithNoEvents() {
		UUID otherBusinessId = UUID.randomUUID();
		List<Event> emptyEvents = ((List<Event>) eventRepository.findByCreatorId(otherBusinessId));

		assertTrue(emptyEvents.isEmpty());
	}

	@Test
	@DisplayName("Combined filters: findByActiveAndCategory returns correct subset")
	void combinedFilters_shouldWorkCorrectly() {
		List<Event> activeCultureEvents = ((List<Event>) eventRepository.findByActiveAndCategory(true, EventCategory.CULTURE));

		assertEquals(2, activeCultureEvents.size());
		assertTrue(activeCultureEvents.stream().allMatch(Event::getActive));
		assertTrue(activeCultureEvents.stream().allMatch(event -> event.getCategory() == EventCategory.CULTURE));
	}
}