package com.edgescheduler.scheduleservice.dto.request;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeletedSchedule {
    Long parentScheduleId;
    Instant deleteStartInstant;
    Instant deleteEndInstant;
}
