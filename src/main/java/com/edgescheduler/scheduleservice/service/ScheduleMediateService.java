package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.vo.ScheduleVO;
import java.time.Instant;
import java.util.List;

public interface ScheduleMediateService {

    boolean isAvailableWithOtherSchedule(
        Instant startTime,
        Instant endTime,
        List<ScheduleVO> schedules
    );

    boolean isOnWorkingHourAndAvailable(
        Instant startTime,
        Instant endTime,
        List<ScheduleVO> schedules
    );
}
