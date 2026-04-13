package com.streetask.app.question;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.streetask.app.functionalities.shared.json.FlexibleInstantDeserializer;
import com.streetask.app.model.GeoPoint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateQuestionRequest {

    @NotBlank(message = "Question title is required")
    private String title;

    @NotBlank(message = "Question content is required")
    private String content;

    @Valid
    private GeoPoint location;

    private Float radiusKm;

    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant expiresAt;

    private EventReference event;

    private Boolean confirmStreetCoinSpend;

    @Getter
    @Setter
    public static class EventReference {
        private UUID id;
    }
}
