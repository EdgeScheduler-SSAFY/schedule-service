package com.edgescheduler.scheduleservice.dto.request;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChangeScheduleTimeRequest {

    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
