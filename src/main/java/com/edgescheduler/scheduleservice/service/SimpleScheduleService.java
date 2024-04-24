package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleCreateResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleUpdateResponse;
import com.edgescheduler.scheduleservice.repository.SimpleScheduleRepository;
import com.edgescheduler.scheduleservice.util.TimeIntervalUtils;
import com.edgescheduler.scheduleservice.vo.ScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class SimpleScheduleService implements ScheduleService {

    private final SimpleScheduleRepository scheduleRepository;
    private final ScheduleMediateService scheduleMediateService;

    @Override
    public ScheduleCreateResponse createSchedule(ScheduleCreateRequest scheduleRequest) {
        return null;
    }

    @Override
    public ScheduleDetailReadResponse getSchedule(Long id) {
        return null;
    }

    @Override
    public ScheduleUpdateResponse updateSchedule(Long scheduleId, ScheduleUpdateRequest scheduleRequest) {
        return null;
    }

    @Override
    public void deleteSchedule(Long id) {

    }

    // TODO: 일정 가용성 계산이 동일한 List를 반복적으로 순회하도록 되어있음. 최적화 필요.
    @Override
    public CalculateAvailabilityResponse calculateAvailability(CalculateAvailabilityRequest calculateAvailabilityRequest) {

        // 주어진 기한 조건의 시작 일시와 끝 일시를 15분 단위(00분 / 15분 / 30분 / 45분)로 조정하고 그 사이의 토큰화된 구간의 개수를 계산한다.
        LocalDateTime adjustedStart = TimeIntervalUtils.adjustToNextQuarterHour(calculateAvailabilityRequest.getStartDatetime());
        LocalDateTime adjustedEnd = TimeIntervalUtils.adjustToPreviousQuarterHour(calculateAvailabilityRequest.getEndDatetime());
        long intervalCount = TimeIntervalUtils.calculateIntervalCount(adjustedStart, adjustedEnd);

        // 조정된 시작 일시와 끝 일시를 UTC 표준시로 변환한다
        Instant start = adjustedStart.toInstant(ZoneOffset.UTC);
        Instant end = adjustedEnd.toInstant(ZoneOffset.UTC);

        // 필참 멤버와 그외 멤버의 스케줄을 조회하여 Map 형태로 저장한다
        Map<Integer, List<ScheduleVO>> requiredMemberScheduleMap = new HashMap<>();
        Map<Integer, List<ScheduleVO>> unrequiredMemberScheduleMap = new HashMap<>();
        calculateAvailabilityRequest.getMemberList()
                .forEach(
                        member -> {
                            if (member.getIsRequired()) {
                                requiredMemberScheduleMap.put(
                                        member.getMemberId(),
                                        scheduleRepository.findByMemberIdAndEndDatetimeBeforeAndStartDatetimeAfter(
                                                member.getMemberId(),
                                                start,
                                                end
                                        )
                                );
                            } else {
                                unrequiredMemberScheduleMap.put(
                                        member.getMemberId(),
                                        scheduleRepository.findByMemberIdAndEndDatetimeBeforeAndStartDatetimeAfter(
                                                member.getMemberId(),
                                                start,
                                                end
                                        )
                                );
                            }
                        }
                );

        // 응답에 필요한 개별 회원의 스케줄 목록을 생성한다
        List<CalculateAvailabilityResponse.individualSchedules> schedules = new ArrayList<>();
        addIndividualSchedules(schedules, requiredMemberScheduleMap);
        addIndividualSchedules(schedules, unrequiredMemberScheduleMap);

        // 토큰화된 시간대별로 필참 멤버와 그외 멤버의 가용성을 계산한다
        List<CalculateAvailabilityResponse.TokenizedTimeAvailability> tokenizedTimeAvailabilities = new ArrayList<>();

        for (long i = 0; i < intervalCount; i++) {

            Instant intervalStart = start.plusSeconds(i * 900);
            Instant intervalEnd = start.plusSeconds((i + 1) * 900);

            int availableMemberCount = (int) unrequiredMemberScheduleMap.values().stream()
                    .filter(scheduleVOList -> scheduleMediateService.isAvailableWithOtherSchedule(intervalStart, intervalEnd, scheduleVOList))
                    .count();
            int availableRequiredMemberCount = availableMemberCount +
                    (int) requiredMemberScheduleMap.values().stream()
                    .filter(scheduleVOList -> scheduleMediateService.isAvailableWithOtherSchedule(intervalStart, intervalEnd, scheduleVOList))
                    .count();
            int availableMemberInWorkingHourCount = (int) unrequiredMemberScheduleMap.values().stream()
                    .filter(scheduleVOList -> scheduleMediateService.isOnWorkingHourAndAvailable(intervalStart, intervalEnd, scheduleVOList))
                    .count();
            int availableRequiredMemberInWorkingHourCount = availableMemberInWorkingHourCount +
                    (int) requiredMemberScheduleMap.values().stream()
                    .filter(scheduleVOList -> scheduleMediateService.isOnWorkingHourAndAvailable(intervalStart, intervalEnd, scheduleVOList))
                    .count();

            tokenizedTimeAvailabilities.add(
                    CalculateAvailabilityResponse.TokenizedTimeAvailability.builder()
                            .availableMemberCount(availableMemberCount)
                            .availableRequiredMemberCount(availableRequiredMemberCount)
                            .availableMemberInWorkingHourCount(availableMemberInWorkingHourCount)
                            .availableRequiredMemberInWorkingHourCount(availableRequiredMemberInWorkingHourCount)
                            .build()
            );
        }

        return CalculateAvailabilityResponse.builder()
                .schedules(schedules)
                .tokenizedTimeAvailabilities(tokenizedTimeAvailabilities)
                .build();
    }

    /**
     *  개별 회원의 스케줄을 추가한다.
     *  TODO: 추후 각 멤버별 시간대 변환 처리 필요
     */
    private void addIndividualSchedules(
            List<CalculateAvailabilityResponse.individualSchedules> schedules,
            Map<Integer, List<ScheduleVO>> memberScheduleMap
    ) {
        memberScheduleMap.keySet()
                .forEach(
                        memberId -> {
                            schedules.add(
                                    CalculateAvailabilityResponse.individualSchedules.builder()
                                            .memberId(memberId)
                                            .schedules(memberScheduleMap.get(memberId).stream().map(
                                                    schedule -> CalculateAvailabilityResponse.ScheduleEntry.builder()
                                                            .name(schedule.name())
                                                            .startDatetime(LocalDateTime.ofInstant(schedule.startDatetime(), ZoneOffset.UTC))
                                                            .endDatetime(LocalDateTime.ofInstant(schedule.endDatetime(), ZoneOffset.UTC))
                                                            .type(schedule.type())
                                                            .isPublic(schedule.isPublic())
                                                            .build()
                                            ).toList())
                                            .build()
                            );
                        }
                );
    }
}
