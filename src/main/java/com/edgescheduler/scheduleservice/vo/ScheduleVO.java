package com.edgescheduler.scheduleservice.vo;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.Instant;

public record ScheduleVO(
    Long id,
    ScheduleType type,
    Instant startDatetime,
    Instant endDatetime
) {}
