package com.edgescheduler.scheduleservice.dto.response;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ScheduleListReadResponse {

    List<IndividualSchedule> scheduleList;

    @Builder
    @Getter
    public static class IndividualSchedule {

        private Long scheduleId;
        private Integer organizerId;
        private String name;
        private ScheduleType type;
        private Integer color;
        private LocalDateTime startDatetime;
        private LocalDateTime endDatetime;
        private Boolean isPublic;
    }
}
