package com.edgescheduler.scheduleservice.dto.response;

import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.LocalDateTime;
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
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private List<ScheduleDetailAttendee> attendeeList;

    @Getter
    @Builder
    public static class ScheduleDetailAttendee {
        private String memberId;
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
}
