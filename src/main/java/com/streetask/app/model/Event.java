package com.streetask.app.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.streetask.app.model.enums.EventCategory;
import com.streetask.app.business.BusinessAccount;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id")
    private BusinessAccount creator;

    @NotBlank(message = "Event title is required")
    private String title;

    @NotBlank(message = "Event description is required")
    private String description;

    private EventCategory category;

    @Embedded
    @Valid
    private GeoPoint location;

    private String address;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private Boolean featured;

    @PositiveOrZero(message = "Attendee count must be zero or positive")
    private Integer attendeeCount;

    @Transient
    private Boolean myAttendance;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "event")
    private List<Question> questions;

    @JsonIgnore
    @OneToMany(mappedBy = "event")
    private List<EventAttendance> attendances;
}
