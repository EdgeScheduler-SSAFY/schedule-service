package com.edgescheduler.scheduleservice.domain;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.TreeSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Recurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RecurrenceFreqType freq;

    private Integer intv;

    private Instant expiredDate;

    private Integer count;

    @Builder.Default
    @Convert(converter = RecurrenceDaySetConverter.class)
    private Set<String> recurrenceDay = new TreeSet<>();

    public void terminateRecurrence(ZoneId zoneId) {
        this.expiredDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).atZone(zoneId).withZoneSameInstant(ZoneOffset.UTC).toInstant();
    }
}
