package com.streetask.app.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.streetask.app.model.Event;
import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.enums.EventCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventCompactDto {

    private UUID id;
    private String title;
    private String description;
    private EventCategory category;
    private GeoPoint location;
    private String address;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Boolean featured;
    private Integer attendeeCount;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EventCreatorSummary creator;
    private Boolean myAttendance;

    @Getter
    @Setter
    public static class EventCreatorSummary {
        private UUID id;
        private String userName;
        private String companyName;
        private String email;

        public EventCreatorSummary(UUID id, String userName, String companyName, String email) {
            this.id = id;
            this.userName = userName;
            this.companyName = companyName;
            this.email = email;
        }
    }

    public static EventCompactDto fromEvent(Event e) {
        EventCompactDto dto = new EventCompactDto();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription());
        dto.setCategory(e.getCategory());
        dto.setLocation(e.getLocation());
        dto.setAddress(e.getAddress());
        dto.setStartsAt(e.getStartsAt());
        dto.setEndsAt(e.getEndsAt());
        dto.setFeatured(e.getFeatured());
        dto.setAttendeeCount(e.getAttendeeCount());
        dto.setActive(e.getActive());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setMyAttendance(e.getMyAttendance());

        if (e.getCreator() != null) {
            dto.setCreator(new EventCreatorSummary(
                    e.getCreator().getId(),
                    e.getCreator().getUserName(),
                    e.getCreator().getCompanyName(),
                    e.getCreator().getEmail()));
        }

        return dto;
    }
}
