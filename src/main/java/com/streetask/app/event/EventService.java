package com.streetask.app.event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.exceptions.ResourceNotOwnedException;
import com.streetask.app.model.Event;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import com.streetask.app.business.BusinessAccount;

import jakarta.validation.Valid;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Autowired
    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Event findEvent(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
    }

    @Transactional(readOnly = true)
    public Iterable<Event> findAll() {
        return eventRepository.findAll();
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
                "questions",
                "attendances");
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
}
