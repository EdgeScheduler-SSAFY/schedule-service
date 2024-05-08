package com.edgescheduler.scheduleservice.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class AlterTimeUtils {

    public static Instant LocalDateTimeToInstant(LocalDateTime localDateTime, ZoneId zoneId) {
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId)
            .withZoneSameInstant(ZoneOffset.UTC);
        return zonedDateTime.toInstant();
    }

    public static LocalDateTime instantToLocalDateTime(Instant instant, ZoneId zoneId) {
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        return zonedDateTime.toLocalDateTime();
    }

    public static LocalDateTime LocalDateTimeToUTCLocalDateTime(LocalDateTime localDateTime, ZoneId zoneId) {
        return localDateTime.atZone(zoneId)
            .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime InstantToUTCLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }
}
