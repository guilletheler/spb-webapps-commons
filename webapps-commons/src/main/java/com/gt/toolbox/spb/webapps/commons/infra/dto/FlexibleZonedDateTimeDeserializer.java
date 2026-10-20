package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class FlexibleZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

    private final ZoneId defaultZoneId;

    public FlexibleZonedDateTimeDeserializer() {
        this(ZoneId.systemDefault());
    }

    public FlexibleZonedDateTimeDeserializer(ZoneId defaultZoneId) {
        this.defaultZoneId = Objects.requireNonNullElseGet(defaultZoneId, ZoneId::systemDefault);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return Instant.ofEpochMilli(p.getLongValue()).atZone(defaultZoneId);
        }

        var text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();
        try {
            return ZonedDateTime.parse(text);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(text).atZone(defaultZoneId);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDate.parse(text).atStartOfDay(defaultZoneId);
                } catch (DateTimeParseException e3) {
                    return (ZonedDateTime) ctxt.handleWeirdStringValue(
                            ZonedDateTime.class,
                            text,
                            "Cannot parse date/time string into ZonedDateTime: %s",
                            e.getMessage());
                }
            }
        }
    }
}
