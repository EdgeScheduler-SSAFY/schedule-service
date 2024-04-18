package com.edgescheduler.scheduleservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Schedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer organizerId;
    private String name;
    private String description;
    private String type;
    private Instant startDatetime;
    private Instant endDatetime;
    private String googleCalendarId;
    private Boolean isPublic;
}
