package com.streetask.app.event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
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
import com.streetask.app.business.BusinessPremiumAccessGuard;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.exceptions.ResourceNotOwnedException;
import com.streetask.app.model.Event;
import com.streetask.app.model.EventAttendance;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final UserRepository userRepository;
    private final BusinessPremiumAccessGuard businessPremiumAccessGuard;

    @Autowired
    public EventService(EventRepository eventRepository, EventAttendanceRepository eventAttendanceRepository,
            UserRepository userRepository, BusinessPremiumAccessGuard businessPremiumAccessGuard) {
        this.eventRepository = eventRepository;
        this.eventAttendanceRepository = eventAttendanceRepository;
        this.userRepository = userRepository;
        this.businessPremiumAccessGuard = businessPremiumAccessGuard;
    }

    @Transactional
    public Event findEvent(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
        deactivateIfExpired(event, LocalDateTime.now());
        applyViewerAttendanceState(event);
        return event;
    }

    @Transactional
    public Iterable<Event> findAll() {
        Iterable<Event> events = eventRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (Event event : events) {
            deactivateIfExpired(event, now);
            applyViewerAttendanceState(event);
        }
        return events;
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
        businessPremiumAccessGuard.requireVerified(creator);
        event.setCreator(creator);
        applyDefaultsOnCreate(event);
        eventRepository.save(event);
        applyViewerAttendanceState(event);
        return event;
    }

    @Transactional
    public Event updateEvent(@Valid Event event, UUID idToUpdate) {
        Event toUpdate = findEvent(idToUpdate);
        BusinessAccount authenticatedBusiness = getAuthenticatedBusinessUser();
        businessPremiumAccessGuard.requireVerified(authenticatedBusiness);

        if (!authenticatedBusiness.getId().equals(toUpdate.getCreator().getId())) {
            throw new ResourceNotOwnedException(toUpdate);
        }

        BeanUtils.copyProperties(event, toUpdate, "id", "creator", "createdAt", "updatedAt", "attendeeCount",
                "questions", "attendances");
        applyDefaultsOnUpdate(toUpdate);
        eventRepository.save(toUpdate);
        applyViewerAttendanceState(toUpdate);
        return toUpdate;
    }

    @Transactional
    public Event toggleAttendance(UUID eventId) {
        Event event = findEvent(eventId);
        RegularUser regularUser = getAuthenticatedRegularUser();

        EventAttendance attendance = eventAttendanceRepository
                .findByRegularUserIdAndEventId(regularUser.getId(), eventId)
                .orElseGet(() -> {
                    EventAttendance newAttendance = new EventAttendance();
                    newAttendance.setRegularUser(regularUser);
                    newAttendance.setEvent(event);
                    return newAttendance;
                });

        boolean attending = !Boolean.TRUE.equals(attendance.getIsAttending());
        attendance.setIsAttending(attending);
        attendance.setConfirmedAt(attending ? LocalDateTime.now(ZoneId.of("UTC")) : null);
        eventAttendanceRepository.save(attendance);

        long attendeeCount = eventAttendanceRepository.countAttendingByEventId(eventId);
        event.setAttendeeCount((int) attendeeCount);
        event.setUpdatedAt(LocalDateTime.now(ZoneId.of("UTC")));
        eventRepository.save(event);

        event.setMyAttendance(attending);
        return event;
    }

    @Transactional(readOnly = true)
    public List<EventAttendeeSummary> findAttendees(UUID eventId) {
        findEvent(eventId);

        return eventAttendanceRepository.findAttendingByEventId(eventId).stream()
                .map(attendance -> {
                    RegularUser user = attendance.getRegularUser();
                    return new EventAttendeeSummary(
                            user.getId(),
                            user.getUserName(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getProfilePhoto());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEvent(UUID id) {
        Event toDelete = findEvent(id);
        BusinessAccount authenticatedBusiness = getAuthenticatedBusinessUser();
        businessPremiumAccessGuard.requireVerified(authenticatedBusiness);

        if (!authenticatedBusiness.getId().equals(toDelete.getCreator().getId())) {
            throw new ResourceNotOwnedException(toDelete);
        }

        eventAttendanceRepository.deleteByEventId(id);
        eventRepository.delete(toDelete);
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

    private RegularUser getAuthenticatedRegularUser() {
        Optional<RegularUser> regularUser = findAuthenticatedRegularUser();
        return regularUser.orElseThrow(
                () -> new AccessDeniedException("Only authenticated regular users can join events"));
    }

    private Optional<RegularUser> findAuthenticatedRegularUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return Optional.empty();
        }

        Optional<User> user = Optional.ofNullable(userRepository.findByEmail(auth.getName().trim()))
                .orElse(Optional.empty());

        return user.filter(RegularUser.class::isInstance).map(RegularUser.class::cast);
    }

    private void applyViewerAttendanceState(Event event) {
        if (event == null) {
            return;
        }

        if (event.getId() == null) {
            event.setMyAttendance(false);
            return;
        }

        Optional<RegularUser> regularUser = findAuthenticatedRegularUser();
        if (regularUser.isEmpty()) {
            event.setMyAttendance(false);
            return;
        }

        boolean attending = eventAttendanceRepository
                .findByRegularUserIdAndEventId(regularUser.get().getId(), event.getId())
                .map(attendance -> Boolean.TRUE.equals(attendance.getIsAttending()))
                .orElse(false);
        event.setMyAttendance(attending);
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