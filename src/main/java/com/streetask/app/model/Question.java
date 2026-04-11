package com.streetask.app.model;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.streetask.app.user.RegularUser;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
public class Question extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id")
    private RegularUser creator;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @NotBlank(message = "Question title is required")
    private String title;

    @NotBlank(message = "Question content is required")
    private String content;

    @Embedded
    @Valid
    private GeoPoint location;

    private Float radiusKm;

    private Boolean active;

    // @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    // @JsonSerialize(using = UtcLocalDateTimeSerializer.class)
    private Instant expiresAt;

    // @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    // @JsonSerialize(using = UtcLocalDateTimeSerializer.class)
    private Instant createdAt;

    private Boolean featured;

    @PositiveOrZero(message = "Answer count must be zero or positive")
    private Integer answerCount;

    @OneToMany(mappedBy = "question")
    @JsonManagedReference("question-answers")
    private List<Answer> answers;
}
