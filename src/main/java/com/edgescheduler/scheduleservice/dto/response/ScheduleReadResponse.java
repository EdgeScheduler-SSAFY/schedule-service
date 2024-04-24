package com.edgescheduler.scheduleservice.dto.response;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleReadResponse {
    private Long scheduleId;
    private Integer organizerId;
    private String name;
    private ScheduleType type;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
