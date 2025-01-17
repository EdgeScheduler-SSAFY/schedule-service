package com.edgescheduler.scheduleservice.dto.request;

import com.edgescheduler.scheduleservice.domain.RecurrenceDayType;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleCreateRequest {

    private Integer organizerId;
    private String name;
    private String description;
    private ScheduleType type;
    private Integer color;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Boolean isPublic;
    private Boolean isRecurrence;
    private RecurrenceDetails recurrence;
    private List<ScheduleAttendee> attendeeList;

    @Getter
    @Builder
    public static class RecurrenceDetails {
        private String freq;
        private Integer intv;
        private LocalDateTime expiredDate;
        private Integer count;
        private List<RecurrenceDayType> recurrenceDay;
    }

    @Getter
    @Builder
    public static class ScheduleAttendee {
        private Integer memberId;
        private Boolean isRequired;
    }
}
