package com.edgescheduler.scheduleservice.dto.request;

import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DecideAttendanceRequest {

    private AttendeeStatus status;
    private String reason;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
