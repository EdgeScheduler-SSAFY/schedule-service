package com.edgescheduler.scheduleservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@Entity
public class Schedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Integer organizerId;

    @NotNull
    private String name;

    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ScheduleType type;

    @NotNull
    private Instant startDatetime;

    @NotNull
    private Instant endDatetime;

    private String googleCalendarId;

    @NotNull
    private Boolean isPublic;

    @Builder
    public Schedule(Integer organizerId, String name, String description, ScheduleType type, Instant startDatetime, Instant endDatetime, String googleCalendarId, Boolean isPublic){
        this.organizerId = organizerId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.googleCalendarId = googleCalendarId;
        this.isPublic = isPublic;
    }
}
