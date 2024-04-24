package com.edgescheduler.scheduleservice.dto.request;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.vo.DayOfWeek;
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
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Boolean isPublic;
    private Boolean isRepeated;
    private RepeatDetails repeat;
    private List<ScheduleAttendee> attendeeList;

    @Getter
    @Builder
    public static class RepeatDetails {
        private String freq;
        private Integer interval;
        private LocalDateTime expiredDate;
        private Integer count;
        private List<DayOfWeek> repeatDay;
    }

    @Getter
    @Builder
    public static class ScheduleAttendee {
        private String memberId;
        private Boolean isRequired;
    }
}
