package com.edgescheduler.scheduleservice.domain;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.EnumSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @NotNull
    private RecurrenceFreqType freq;

    @NotNull
    private Integer intv;

    private Instant expiredDate;

    private Integer count;

    @Convert(converter = RecurrenceDaySetConverter.class)
    private EnumSet<RecurrenceDayType> recurrenceDay;

    public void terminateRecurrenceByDate(Instant expiredInstant) {
        this.expiredDate = expiredInstant;
    }
}
