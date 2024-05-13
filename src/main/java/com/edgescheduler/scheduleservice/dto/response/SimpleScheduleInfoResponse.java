package com.edgescheduler.scheduleservice.dto.response;

import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimpleScheduleInfoResponse {

    private Long scheduleId;
    private String name;
    private Integer organizerId;
    private String organizerName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Integer runningTime;
    private AttendeeStatus receiverStatus;
}
