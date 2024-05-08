package com.edgescheduler.scheduleservice.dto.response;

import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import com.edgescheduler.scheduleservice.domain.RecurrenceDayType;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleDetailReadResponse {

    private Long scheduleId;
    private Integer organizerId;
    private String name;
    private String description;
    private ScheduleType type;
    private Integer color;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private List<ScheduleDetailAttendee> attendeeList;
    private Boolean isPublic;
    private RecurrenceDetails recurrenceDetails;

    @Getter
    @Builder
    public static class ScheduleDetailAttendee {
        private Integer memberId;
        private String memberName;
        private Boolean isRequired;
        private AttendeeStatus status;
        private String reason;
        private ScheduleProposal proposal;
    }

    @Getter
    @Builder
    public static class ScheduleProposal {
        private Long proposalId;
        private LocalDateTime startDatetime;
        private LocalDateTime endDatetime;
    }

    @Getter
    @Builder
    public static class RecurrenceDetails {
        private String freq;
        private Integer intv;
        private LocalDateTime expiredDate;
        private Integer count;
        private EnumSet<RecurrenceDayType> recurrenceDay;
    }
}
