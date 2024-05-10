package com.edgescheduler.scheduleservice.controller;

import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityWithProposalRequest;
import com.edgescheduler.scheduleservice.dto.request.DecideAttendanceRequest;
import com.edgescheduler.scheduleservice.dto.request.ResponseScheduleProposal;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleDeleteRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.dto.response.ScheduleCreateResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleUpdateResponse;
import com.edgescheduler.scheduleservice.service.ScheduleMediateService;
import com.edgescheduler.scheduleservice.service.ScheduleService;
import java.net.URI;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleMediateService scheduleMediateService;

    @PostMapping
    public ResponseEntity<ScheduleCreateResponse> createSchedule(
        @RequestBody ScheduleCreateRequest scheduleRequest
    ) {
        var response = scheduleService.createSchedule(scheduleRequest);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getScheduleId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleDetailReadResponse> getSchedule(
        @RequestHeader(name = "Authorization", defaultValue = "1") Integer memberId,
        @PathVariable Long id
    ) {
        var response = scheduleService.getSchedule(memberId, id);
        return ResponseEntity.ok(response);
    }

    // TODO: 기간별 일정 조회
    @GetMapping("/period")
    public ResponseEntity<ScheduleListReadResponse> getSchedulesByPeriod(
        @RequestHeader(name = "Authorization", defaultValue = "1") Integer memberId,
        @RequestParam(required = true) LocalDateTime startDatetime,
        @RequestParam(required = true) LocalDateTime endDatetime) {
        var scheduleListReadResponse = scheduleService.getScheduleByPeriod(memberId, startDatetime,
            endDatetime);
        return ResponseEntity.ok(scheduleListReadResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleUpdateResponse> updateSchedule(
        @RequestHeader(name = "Authorization", defaultValue = "1") Integer memberId,
        @PathVariable Long id,
        @RequestBody ScheduleUpdateRequest scheduleRequest
    ) {
        var scheduleUpdateResponse = scheduleService.updateSchedule(memberId, id, scheduleRequest);
        return ResponseEntity.ok(scheduleUpdateResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(
        @RequestHeader(name = "Authorization") Integer memberId,
        @PathVariable Long id,
        @RequestBody ScheduleDeleteRequest scheduleDeleteRequest
    ) {
        scheduleService.deleteSchedule(memberId, id, scheduleDeleteRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/members/calculate-time-availability")
    public ResponseEntity<?> calculateTimeAvailability(
        @RequestHeader(name = "Authorization") Integer memberId,
        @RequestBody CalculateAvailabilityRequest calculateAvailabilityRequest
    ) {
        calculateAvailabilityRequest.setOrganizerId(memberId);
        var response = scheduleMediateService.calculateAvailability(calculateAvailabilityRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate-time-availability-with-proposal")
    public ResponseEntity<?> calculateTimeAvailabilityWithProposal(
        @RequestHeader(name = "Authorization") Integer memberId,
        @RequestBody CalculateAvailabilityWithProposalRequest calculateAvailabilityRequest
    ) {
        calculateAvailabilityRequest.setRetrieverId(memberId);
        var response = scheduleMediateService.calculateAvailableMembersWithProposedSchedule(
            calculateAvailabilityRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{scheduleId}/members/attendance")
    public ResponseEntity<Void> decideAttendance(
        @PathVariable Long scheduleId,
        @RequestHeader(name = "Authorization") Integer memberId,
        @RequestBody DecideAttendanceRequest decideAttendanceRequest
    ) {
        scheduleService.decideAttendance(scheduleId, memberId, decideAttendanceRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{scheduleId}/{proposalId}")
    public ResponseEntity<Void> responseScheduleProposal(
        @PathVariable Long scheduleId,
        @PathVariable Long proposalId,
        @RequestHeader(name = "Authorization") Integer memberId,
        @RequestBody ResponseScheduleProposal responseScheduleProposal
    ) {
        scheduleService.responseScheduleProposal(scheduleId, memberId, proposalId,
            responseScheduleProposal);
        return ResponseEntity.ok().build();
    }
}
