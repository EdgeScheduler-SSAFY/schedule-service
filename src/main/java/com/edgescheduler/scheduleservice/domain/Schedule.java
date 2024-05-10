package com.edgescheduler.scheduleservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @NotNull
    private Integer color;

    @NotNull
    @ColumnDefault("false")
    private Boolean isDeleted;

    @Setter
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
    private List<Attendee> attendees;

    @Setter
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "recurrence_id")
    private Recurrence recurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_schedule_id")
    private Schedule parentSchedule;

    public void updateNotRecurrencePrivateSchedule(Integer organizerId, String name,
        String description,
        ScheduleType type, Instant startDatetime, Instant endDatetime, Boolean isPublic,
        Integer color) {
        this.organizerId = organizerId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.isPublic = isPublic;
        this.color = color;
    }

    public void updateMeetingSchedule(String name, String description,
        ScheduleType type, Instant startDatetime, Instant endDatetime, Boolean isPublic,
        Integer color, List<Attendee> attendees) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.isPublic = isPublic;
        this.color = color;
        this.attendees = attendees;
    }

    public void changeScheduleTime(Instant startDatetime, Instant endDatetime
    ) {
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
    }
}
