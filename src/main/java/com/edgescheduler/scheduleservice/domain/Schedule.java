package com.edgescheduler.scheduleservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

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
    private ScheduleType type;

    @NotNull
    private Instant startDatetime;

    @NotNull
    private Instant endDatetime;

    private String googleCalendarId;

    @NotNull
    private Boolean isPublic;
}
