package com.edgescheduler.scheduleservice.vo;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.Instant;
import lombok.Builder;

@Builder
public record ScheduleVO(
    Long id,
    String name,
    ScheduleType type,
    Instant startDatetime,
    Instant endDatetime,
    Boolean isPublic
) {

}
