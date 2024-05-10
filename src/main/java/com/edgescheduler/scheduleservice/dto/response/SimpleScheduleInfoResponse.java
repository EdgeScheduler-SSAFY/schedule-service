package com.edgescheduler.scheduleservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SimpleScheduleInfoResponse {
    private Long scheduleId;
    private String name;
    private Integer organizerId;
    private String organizerName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
