package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleCreateResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleUpdateResponse;
import java.time.LocalDateTime;

public interface ScheduleService {

    ScheduleCreateResponse createSchedule(ScheduleCreateRequest scheduleRequest);

    ScheduleDetailReadResponse getSchedule(Integer memberId, Long id);

    ScheduleListReadResponse getScheduleByPeriod(LocalDateTime start, LocalDateTime end);

    ScheduleUpdateResponse updateSchedule(
        Integer memberId,
        Long scheduleId,
        ScheduleUpdateRequest scheduleRequest);

    void deleteSchedule(Long id);

    CalculateAvailabilityResponse calculateAvailability(
        CalculateAvailabilityRequest calculateAvailabilityRequest);
}
