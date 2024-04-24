package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.vo.ScheduleVO;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SimpleScheduleMediateService implements ScheduleMediateService {

    @Override
    public boolean isAvailableWithOtherSchedule(Instant startTime, Instant endTime,
        List<ScheduleVO> schedules) {
        return schedules.stream().noneMatch(schedule -> isTimeConflict(startTime, endTime, schedule));
    }

    @Override
    public boolean isOnWorkingHourAndAvailable(Instant startTime, Instant endTime,
        List<ScheduleVO> schedules) {
        return schedules.stream().noneMatch(schedule -> isTimeConflict(startTime, endTime, schedule)) &&
            schedules.stream().anyMatch(schedule -> isWithinWorkingHour(startTime, endTime, schedule));
    }

    private boolean isTimeConflict(Instant startTime, Instant endTime, ScheduleVO schedule) {
        return !schedule.type().equals(ScheduleType.WORKING) &&
            startTime.isBefore(schedule.endDatetime()) && endTime.isAfter(schedule.startDatetime());
    }

    private boolean isWithinWorkingHour(Instant startTime, Instant endTime, ScheduleVO schedule) {
        return schedule.type().equals(ScheduleType.WORKING) &&
            !startTime.isBefore(schedule.startDatetime()) && !endTime.isAfter(schedule.endDatetime());
    }
}
