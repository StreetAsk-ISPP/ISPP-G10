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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.exceptions.ResourceNotOwnedException;
import com.streetask.app.model.Event;
import com.streetask.app.model.EventAttendance;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventAttendanceRepository eventAttendanceRepository;

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
    void toggleAttendance_shouldCreateAttendanceAndIncrementCountForAuthenticatedRegularUser() {
        UUID eventId = UUID.randomUUID();
        RegularUser regularUser = new RegularUser();
        regularUser.setId(UUID.randomUUID());
        regularUser.setEmail("regular@streetask.com");

        Event event = new Event();
        event.setId(eventId);
        event.setCreator(owner);
        event.setTitle("Attendance Event");
        event.setDescription("Attendance toggle test");
        event.setAttendeeCount(0);

        authenticateAs(regularUser.getEmail());
        when(userRepository.findByEmail(regularUser.getEmail())).thenReturn(Optional.of(regularUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttendanceRepository.findByRegularUserIdAndEventId(regularUser.getId(), eventId))
                .thenReturn(Optional.empty());
        when(eventAttendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventAttendanceRepository.countAttendingByEventId(eventId)).thenReturn(1L);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventService.toggleAttendance(eventId);

        assertTrue(updated.getMyAttendance());
        assertEquals(1, updated.getAttendeeCount());
        verify(eventAttendanceRepository).save(any(EventAttendance.class));
        verify(eventRepository).save(event);
    }

    @Test
    void toggleAttendance_shouldRemoveAttendanceAndDecrementCountForAuthenticatedRegularUser() {
        UUID eventId = UUID.randomUUID();
        RegularUser regularUser = new RegularUser();
        regularUser.setId(UUID.randomUUID());
        regularUser.setEmail("regular@streetask.com");

        Event event = new Event();
        event.setId(eventId);
        event.setCreator(owner);
        event.setTitle("Attendance Event");
        event.setDescription("Attendance toggle test");
        event.setAttendeeCount(1);

        EventAttendance attendance = new EventAttendance();
        attendance.setId(UUID.randomUUID());
        attendance.setRegularUser(regularUser);
        attendance.setEvent(event);
        attendance.setIsAttending(true);

        authenticateAs(regularUser.getEmail());
        when(userRepository.findByEmail(regularUser.getEmail())).thenReturn(Optional.of(regularUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttendanceRepository.findByRegularUserIdAndEventId(regularUser.getId(), eventId))
                .thenReturn(Optional.of(attendance));
        when(eventAttendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventAttendanceRepository.countAttendingByEventId(eventId)).thenReturn(0L);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventService.toggleAttendance(eventId);

        assertFalse(updated.getMyAttendance());
        assertEquals(0, updated.getAttendeeCount());
        verify(eventAttendanceRepository).save(any(EventAttendance.class));
        verify(eventRepository).save(event);
    }

    @Test
    void findAttendees_shouldReturnAttendingUsersForOwnedEvent() {
        UUID eventId = UUID.randomUUID();
        RegularUser attendee = new RegularUser();
        attendee.setId(UUID.randomUUID());
        attendee.setEmail("attendee@streetask.com");
        attendee.setUserName("attendee");
        attendee.setFirstName("Att");
        attendee.setLastName("Endee");

        Event event = new Event();
        event.setId(eventId);
        event.setCreator(owner);
        event.setTitle("Attendance Event");
        event.setDescription("Attendance list test");

        EventAttendance attendance = new EventAttendance();
        attendance.setId(UUID.randomUUID());
        attendance.setRegularUser(attendee);
        attendance.setEvent(event);
        attendance.setIsAttending(true);
        attendance.setConfirmedAt(LocalDateTime.now());

        authenticateAs(owner.getEmail());
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventAttendanceRepository.findAttendingByEventId(eventId)).thenReturn(List.of(attendance));

        List<EventAttendeeSummary> attendees = eventService.findAttendees(eventId);

        assertEquals(1, attendees.size());
        assertEquals(attendee.getId(), attendees.get(0).id());
        assertEquals("attendee", attendees.get(0).userName());
        assertEquals("Att", attendees.get(0).firstName());
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

        verify(eventAttendanceRepository).deleteByEventId(eventId);
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