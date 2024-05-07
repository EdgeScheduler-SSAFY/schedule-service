package com.edgescheduler.scheduleservice.dto.request;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdatedSchedule {
    Long parentScheduleId;
    Instant updateDateInstant;
}
