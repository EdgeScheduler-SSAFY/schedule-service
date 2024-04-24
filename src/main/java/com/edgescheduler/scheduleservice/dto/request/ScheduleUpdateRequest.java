package com.edgescheduler.scheduleservice.dto.request;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleUpdateRequest {
    private String name;
    private String description;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
