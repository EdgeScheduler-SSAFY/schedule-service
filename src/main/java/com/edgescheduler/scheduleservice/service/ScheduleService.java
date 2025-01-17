package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.dto.request.DecideAttendanceRequest;
import com.edgescheduler.scheduleservice.dto.request.ResponseScheduleProposal;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleDeleteRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.dto.response.*;

import java.time.LocalDateTime;

public interface ScheduleService {

    ScheduleCreateResponse createSchedule(ScheduleCreateRequest scheduleRequest);

    ScheduleDetailReadResponse getSchedule(Integer memberId, Long id);

    SimpleScheduleInfoResponse getSimpleSchedule(Long scheduleId, Integer receiverId);

    ScheduleListReadResponse getScheduleByPeriod(Integer memberId, LocalDateTime start,
        LocalDateTime end);

    ScheduleUpdateResponse updateSchedule(
        Integer memberId,
        Long scheduleId,
        ScheduleUpdateRequest scheduleRequest);

    void deleteSchedule(Integer memberId, Long id, ScheduleDeleteRequest scheduleDeleteRequest);

    void decideAttendance(Long scheduleId, Integer memberId,
        DecideAttendanceRequest decideAttendanceRequest);

    void responseScheduleProposal(Long scheduleId, Integer memberId, Long proposalId,
        ResponseScheduleProposal responseScheduleProposal);
}
