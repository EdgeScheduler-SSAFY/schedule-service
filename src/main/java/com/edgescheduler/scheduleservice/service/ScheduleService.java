package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.dto.request.ChangeScheduleTimeRequest;
import com.edgescheduler.scheduleservice.dto.request.DecideAttendanceRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleDeleteRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.dto.response.ScheduleCreateResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleUpdateResponse;
import java.time.LocalDateTime;

public interface ScheduleService {

    ScheduleCreateResponse createSchedule(ScheduleCreateRequest scheduleRequest);

    ScheduleDetailReadResponse getSchedule(Integer memberId, Long id);

    ScheduleListReadResponse getScheduleByPeriod(Integer memberId, LocalDateTime start,
        LocalDateTime end);

    ScheduleUpdateResponse updateSchedule(
        Integer memberId,
        Long scheduleId,
        ScheduleUpdateRequest scheduleRequest);

    void deleteSchedule(Integer memberId, Long id, ScheduleDeleteRequest scheduleDeleteRequest);

    void decideAttendance(Long scheduleId, Integer memberId,
        DecideAttendanceRequest decideAttendanceRequest);

    void changeScheduleTime(Integer memberId, Long scheduleId,
        ChangeScheduleTimeRequest changeScheduleTimeRequest);
}
