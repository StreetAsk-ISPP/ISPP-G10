package com.streetask.app.question;

import java.time.Instant;
import java.util.UUID;

import com.streetask.app.model.GeoPoint;
import com.streetask.app.model.Question;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionCompactDto {

    private UUID id;
    private String title;
    private String content;
    private GeoPoint location;
    private Float radiusKm;
    private Boolean active;
    private Instant expiresAt;
    private Instant createdAt;
    private Boolean featured;
    private Integer answerCount;
    private UUID eventId;

    public QuestionCompactDto() {
    }

    public static QuestionCompactDto fromQuestion(Question q) {
        QuestionCompactDto dto = new QuestionCompactDto();
        dto.setId(q.getId());
        dto.setTitle(q.getTitle());
        dto.setContent(q.getContent());
        dto.setLocation(q.getLocation());
        dto.setRadiusKm(q.getRadiusKm());
        dto.setActive(q.getActive());
        dto.setExpiresAt(q.getExpiresAt());
        dto.setCreatedAt(q.getCreatedAt());
        dto.setFeatured(q.getFeatured());
        dto.setAnswerCount(q.getAnswerCount());
        dto.setEventId(q.getEvent() != null ? q.getEvent().getId() : null);
        return dto;
    }
}
