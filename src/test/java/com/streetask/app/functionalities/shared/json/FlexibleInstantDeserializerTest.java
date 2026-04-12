package com.streetask.app.functionalities.shared.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

class FlexibleInstantDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseIsoInstant() throws Exception {
        String json = "{\"expiresAt\":\"2026-03-15T10:00:00Z\"}";

        DatePayload payload = objectMapper.readValue(json, DatePayload.class);

        assertThat(payload.expiresAt).isEqualTo(Instant.parse("2026-03-15T10:00:00Z"));
    }

    @Test
    void shouldParseIsoOffsetDateTime() throws Exception {
        String json = "{\"expiresAt\":\"2026-03-15T11:00:00+01:00\"}";

        DatePayload payload = objectMapper.readValue(json, DatePayload.class);

        assertThat(payload.expiresAt).isEqualTo(Instant.parse("2026-03-15T10:00:00Z"));
    }

    @Test
    void shouldParseIsoLocalDateTimeAsUtc() throws Exception {
        String json = "{\"expiresAt\":\"2026-03-15T11:00:00\"}";

        DatePayload payload = objectMapper.readValue(json, DatePayload.class);

        assertThat(payload.expiresAt).isEqualTo(Instant.parse("2026-03-15T11:00:00Z"));
    }

    private static class DatePayload {
        @JsonDeserialize(using = FlexibleInstantDeserializer.class)
        private Instant expiresAt;
    }
}
