package com.streetask.app.functionalities.shared.json;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Accepts multiple datetime payload formats and normalizes them to Instant.
 * Local datetime values without timezone are interpreted as UTC.
 */
public class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

    private static final DateTimeFormatter SPACE_SEPARATED_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SPACE_SEPARATED_MILLIS = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String rawValue = parser.getValueAsString();
        if (rawValue == null) {
            return null;
        }

        String value = rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value, SPACE_SEPARATED_SECONDS).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value, SPACE_SEPARATED_MILLIS).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        throw InvalidFormatException.from(
                parser,
                "Unsupported datetime format. Expected ISO instant/offset/local datetime.",
                rawValue,
                Instant.class);
    }
}
