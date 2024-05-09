package com.edgescheduler.scheduleservice.service;

import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.calculateAdjustedIntervalCount;
import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.calculateIntervalCount;
import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.calculateIntervalIndexWithinPeriod;
import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.getMinuteDuration;

import com.edgescheduler.scheduleservice.domain.MemberTimezone;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityWithProposalRequest;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityWithProposalResponse;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityWithProposalResponse.AvailableMember;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse.IndividualSchedule;
import com.edgescheduler.scheduleservice.exception.ErrorCode;
import com.edgescheduler.scheduleservice.repository.MemberTimezoneRepository;
import com.edgescheduler.scheduleservice.service.SimpleScheduleService.MeetingRecommendation;
import com.edgescheduler.scheduleservice.service.SimpleScheduleService.MeetingRecommendation.RecommendType;
import com.edgescheduler.scheduleservice.util.AlterTimeUtils;
import com.edgescheduler.scheduleservice.util.TimeIntervalUtils.IntervalIndex;
import com.edgescheduler.scheduleservice.vo.IntervalStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleMediateServiceImpl implements ScheduleMediateService {

    private final ScheduleService scheduleService;
    private final MemberTimezoneRepository memberTimezoneRepository;

    @Override
    public CalculateAvailabilityWithProposalResponse calculateAvailableMembersWithProposedSchedule(
        CalculateAvailabilityWithProposalRequest calculateAvailabilityWithProposalRequest) {

        var retrieverId = calculateAvailabilityWithProposalRequest.getRetrieverId();
        var scheduleId = calculateAvailabilityWithProposalRequest.getScheduleId();
        var startTime = calculateAvailabilityWithProposalRequest.getStartDatetime();
        var endTime = calculateAvailabilityWithProposalRequest.getEndDatetime();

        ScheduleDetailReadResponse schedule = scheduleService.getSchedule(retrieverId, scheduleId);

        // 기존 일정과 총 회의 시간이 일치하지 않으면 에러
        if (getMinuteDuration(startTime, endTime)
            != getMinuteDuration(schedule.getStartDatetime(), schedule.getEndDatetime())) {
            throw ErrorCode.INVALID_PROPOSAL.build();
        }

        MemberTimezone retrieverTimezone = memberTimezoneRepository.findById(retrieverId)
            .orElseThrow(ErrorCode.TIMEZONE_NOT_FOUND::build);
        long intervalCount = calculateIntervalCount(startTime, endTime);
        Instant start = AlterTimeUtils.LocalDateTimeToInstant(startTime,
            ZoneId.of(retrieverTimezone.getZoneId()));
        Instant end = AlterTimeUtils.LocalDateTimeToInstant(endTime,
            ZoneId.of(retrieverTimezone.getZoneId()));

        List<AvailableMember> availableMembers = new ArrayList<>();
        List<AvailableMember> unavailableMembers = new ArrayList<>();

        schedule.getAttendeeList().forEach(attendee -> {
            ZoneId zoneId = ZoneId.of(
                memberTimezoneRepository.findById(attendee.getMemberId())
                    .orElseThrow(ErrorCode.TIMEZONE_NOT_FOUND::build).getZoneId());
            LocalDateTime zonedStart = AlterTimeUtils.instantToLocalDateTime(start, zoneId);
            LocalDateTime zonedEnd = AlterTimeUtils.instantToLocalDateTime(end, zoneId);
            List<IndividualSchedule> schedules = scheduleService.getScheduleByPeriod(
                attendee.getMemberId(),
                zonedStart,
                zonedEnd).getScheduleList();
            IntervalStatus[] availability = getAvailabilityWithinPeriod(
                (int) intervalCount, schedules, zonedStart, zonedEnd);
            if (Arrays.stream(availability)
                .allMatch(status -> status != IntervalStatus.UNAVAILABLE)) {
                availableMembers.add(AvailableMember.builder()
                    .memberId(attendee.getMemberId())
                    .memberName("default")              // TODO: memberName 추가
                    .isRequired(attendee.getIsRequired())
                    .build());
            } else {
                unavailableMembers.add(AvailableMember.builder()
                    .memberId(attendee.getMemberId())
                    .memberName("default")              // TODO: memberName 추가
                    .isRequired(attendee.getIsRequired())
                    .build());
            }
        });

        return CalculateAvailabilityWithProposalResponse.builder()
            .availableMembers(availableMembers)
            .unavailableMembers(unavailableMembers)
            .build();
    }

    public IntervalStatus[] getAvailabilityWithinPeriod(int intervalCount,
        List<IndividualSchedule> schedules,
        LocalDateTime zonedStart, LocalDateTime zonedEnd) {
        IntervalStatus[] availability = new IntervalStatus[intervalCount];
        Arrays.fill(availability, IntervalStatus.AVAILABLE);
        for (IndividualSchedule schedule : schedules) {
            int startAffectedIndex = calculateAffectedIndex(
                calculateIntervalIndexWithinPeriod(
                    zonedStart, zonedEnd,
                    schedule.getStartDatetime()),
                schedule.getType(), true);
            int endAffectedIndex = calculateAffectedIndex(
                calculateIntervalIndexWithinPeriod(
                    zonedStart, zonedEnd,
                    schedule.getEndDatetime()),
                schedule.getType(), false);
            for (int i = startAffectedIndex; i <= endAffectedIndex; i++) {
                if (schedule.getType() != ScheduleType.WORKING) {
                    availability[i] = IntervalStatus.UNAVAILABLE;
                } else if (availability[i] == IntervalStatus.AVAILABLE) {
                    availability[i] = IntervalStatus.AVAILABLE_IN_WORKING_HOURS;
                }
            }
        }
        return availability;
    }

    private boolean conflictedWithPersonalSchedule(LocalDateTime start, LocalDateTime end,
        IndividualSchedule schedule) {
        return schedule.getType() != ScheduleType.WORKING
            && start.isBefore(schedule.getEndDatetime())
            && end.isAfter(schedule.getStartDatetime());
    }

    private boolean isWithinWorkingHour(LocalDateTime start, LocalDateTime end,
        IndividualSchedule schedule) {
        return schedule.getType() == ScheduleType.WORKING
            && start.isAfter(schedule.getStartDatetime())
            && end.isBefore(schedule.getEndDatetime());
    }

    // TODO: 일정 가용성 계산이 동일한 List를 반복적으로 순회하도록 되어있음. 최적화 필요.
    @Override
    public CalculateAvailabilityResponse calculateAvailability(
        CalculateAvailabilityRequest calculateAvailabilityRequest) {

        // 주어진 기한 조건의 시작 일시와 끝 일시 사이의 토큰화된 구간의 개수를 계산한다.
        long intervalCount = calculateAdjustedIntervalCount(
            calculateAvailabilityRequest.getStartDatetime(),
            calculateAvailabilityRequest.getEndDatetime());

        if (intervalCount < calculateAvailabilityRequest.getRunningTime() / 15) {
            throw ErrorCode.INVALID_INTERVAL_COUNT.build();
        }

        MemberTimezone organizerTimezone = memberTimezoneRepository.findById(
                calculateAvailabilityRequest.getOrganizerId())
            .orElseThrow(ErrorCode.TIMEZONE_NOT_FOUND::build);

        // 조정된 시작 일시와 끝 일시를 UTC 표준시로 변환한다
        Instant start = AlterTimeUtils.LocalDateTimeToInstant(
            calculateAvailabilityRequest.getStartDatetime(),
            ZoneId.of(organizerTimezone.getZoneId()));
        Instant end = AlterTimeUtils.LocalDateTimeToInstant(
            calculateAvailabilityRequest.getEndDatetime(),
            ZoneId.of(organizerTimezone.getZoneId()));

        Map<Integer, IntervalStatus[]> requiredMemberAvailabilityMap = new HashMap<>();
        Map<Integer, IntervalStatus[]> optionalMemberAvailabilityMap = new HashMap<>();
        calculateAvailabilityRequest.getMemberList().forEach(member -> {
            ZoneId zoneId = ZoneId.of(
                memberTimezoneRepository.findById(member.getMemberId())
                    .orElseThrow(ErrorCode.TIMEZONE_NOT_FOUND::build).getZoneId());
            LocalDateTime zonedStart = AlterTimeUtils.instantToLocalDateTime(start, zoneId);
            LocalDateTime zonedEnd = AlterTimeUtils.instantToLocalDateTime(end, zoneId);
            List<IndividualSchedule> schedules = scheduleService.getScheduleByPeriod(
                member.getMemberId(),
                zonedStart,
                zonedEnd).getScheduleList();
            IntervalStatus[] availability = getAvailabilityWithinPeriod(
                (int) intervalCount, schedules, zonedStart, zonedEnd);
            if (member.getIsRequired()) {
                requiredMemberAvailabilityMap.put(member.getMemberId(), availability);
            } else {
                optionalMemberAvailabilityMap.put(member.getMemberId(), availability);
            }
        });

//
//        // 응답에 필요한 개별 회원의 스케줄 목록을 생성한다
//        List<IndividualSchedules> schedules = new ArrayList<>();
//        addIndividualSchedules(schedules, requiredMemberScheduleMap);
//        addIndividualSchedules(schedules, unrequiredMemberScheduleMap);
//
//        // 토큰화된 시간대별로 필참 멤버와 그외 멤버의 가용성을 계산한다
//        List<TokenizedTimeAvailability> tokenizedTimeAvailabilities = new ArrayList<>();
//
//        for (long i = 0; i < intervalCount; i++) {
//
//            Instant intervalStart = start.plusSeconds(i * 900);
//            Instant intervalEnd = start.plusSeconds((i + 1) * 900);
//
//            int availableRequiredMemberCount = (int) requiredMemberScheduleMap.values().stream()
//                .filter(scheduleVOList -> scheduleMediateService.isAvailableWithOtherSchedule(
//                    intervalStart, intervalEnd, scheduleVOList)).count();
//            int availableMemberCount =
//                availableRequiredMemberCount + (int) unrequiredMemberScheduleMap.values()
//                    .stream()
//                    .filter(
//                        scheduleVOList -> scheduleMediateService.isAvailableWithOtherSchedule(
//                            intervalStart, intervalEnd, scheduleVOList)).count();
//            int availableRequiredMemberInWorkingHourCount = (int) requiredMemberScheduleMap.values()
//                .stream().filter(
//                    scheduleVOList -> scheduleMediateService.isOnWorkingHourAndAvailable(
//                        intervalStart, intervalEnd, scheduleVOList)).count();
//            int availableMemberInWorkingHourCount = availableRequiredMemberInWorkingHourCount
//                + (int) unrequiredMemberScheduleMap.values().stream().filter(
//                scheduleVOList -> scheduleMediateService.isOnWorkingHourAndAvailable(
//                    intervalStart,
//                    intervalEnd, scheduleVOList)).count();
//
//            tokenizedTimeAvailabilities.add(
//                TokenizedTimeAvailability.builder().availableMemberCount(availableMemberCount)
//                    .availableRequiredMemberCount(availableRequiredMemberCount)
//                    .availableMemberInWorkingHourCount(availableMemberInWorkingHourCount)
//                    .availableRequiredMemberInWorkingHourCount(
//                        availableRequiredMemberInWorkingHourCount).build());
//        }
//
//        return CalculateAvailabilityResponse.builder().schedules(schedules)
//            .tokenizedTimeAvailabilities(tokenizedTimeAvailabilities).build();
        return null;
    }

    /**
     * 필수 참여자가 모두 참여할 수 있는 가장 빠른 회의 시간을 찾는다.
     */
    public MeetingRecommendation findFastestMeeting(
        Map<Integer, IntervalStatus[]> requiredMemberAvailabilityMap,
        Map<Integer, IntervalStatus[]> optionalMemberAvailabilityMap,
        LocalDateTime startDatetime,
        int runningIntervalCount, int intervalCount) {

        int fastestStartIndex = -1;
        int availableCount = 0;
        for (int i = 0; i < intervalCount; i++) {
            int finalI = i;
            if (requiredMemberAvailabilityMap.values().stream()
                .allMatch(availability -> availability[finalI] != IntervalStatus.UNAVAILABLE)) {
                availableCount++;
            } else {
                availableCount = 0;
            }

            if (availableCount == runningIntervalCount) {
                fastestStartIndex = i - runningIntervalCount + 1;
                break;
            }
        }

        if (fastestStartIndex < 0) {
            return null;
        }

        List<Integer> availableMemberIds = new ArrayList<>();
        List<Integer> availableMemberInWorkingHourIds = new ArrayList<>();

        int finalFastestStartIndex = fastestStartIndex;
        requiredMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalFastestStartIndex,
                    finalFastestStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] != IntervalStatus.UNAVAILABLE);
            if (available) {
                availableMemberIds.add(id);
            }
        });
        optionalMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalFastestStartIndex,
                    finalFastestStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] != IntervalStatus.UNAVAILABLE);
            if (available) {
                availableMemberIds.add(id);
            }
        });

        requiredMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalFastestStartIndex,
                    finalFastestStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS);
            if (available) {
                availableMemberInWorkingHourIds.add(id);
            }
        });
        optionalMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalFastestStartIndex,
                    finalFastestStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS);
            if (available) {
                availableMemberInWorkingHourIds.add(id);
            }
        });

        return MeetingRecommendation.builder()
            .recommendType(RecommendType.FASTEST)
            .start(startDatetime.plusMinutes(fastestStartIndex * 15L))
            .end(startDatetime.plusMinutes((fastestStartIndex + runningIntervalCount) * 15L))
            .startIndex(fastestStartIndex)
            .endIndex(fastestStartIndex + runningIntervalCount - 1)
            .availableMemberIds(availableMemberIds)
            .availableMemberInWorkingHourIds(availableMemberInWorkingHourIds)
            .build();
    }

    private void printWindows(int[] requiredCountingWindow, int[] optionalCountingWindow) {
        log.info("requiredCountingWindow: {}", Arrays.toString(requiredCountingWindow));
        log.info("optionalCountingWindow: {}", Arrays.toString(optionalCountingWindow));
    }

    /**
     * Sliding-Window 알고리즘을 사용하여 가장 많은 참여자가 가능한 회의 시간을 찾는다.
     */
    public MeetingRecommendation findMostParticipantsMeeting(
        Map<Integer, IntervalStatus[]> requiredMemberAvailabilityMap,
        Map<Integer, IntervalStatus[]> optionalMemberAvailabilityMap,
        LocalDateTime startDatetime,
        int runningIntervalCount, int intervalCount) {

        int mostParticipantStartIndex = -1;
        int mostParticipantCount = 0;
        int[] requiredCountingWindow = new int[requiredMemberAvailabilityMap.size()];
        int[] optionalCountingWindow = new int[optionalMemberAvailabilityMap.size()];

        Map<Integer, Integer> requiredMemberIndexMap = new HashMap<>();
        Map<Integer, Integer> optionalMemberIndexMap = new HashMap<>();

        int requiredIndex = 0;
        for (Integer key : requiredMemberAvailabilityMap.keySet()) {
            requiredMemberIndexMap.put(key, requiredIndex++);
        }

        int optionalIndex = 0;
        for (Integer key : optionalMemberAvailabilityMap.keySet()) {
            optionalMemberIndexMap.put(key, optionalIndex++);
        }

        for (int i = 0; i < runningIntervalCount; i++) {
            int finalI = i;
            requiredMemberAvailabilityMap.forEach((key, value) -> {
                if (value[finalI] != IntervalStatus.UNAVAILABLE) {
                    requiredCountingWindow[requiredMemberIndexMap.get(key)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((key, value) -> {
                if (value[finalI] != IntervalStatus.UNAVAILABLE) {
                    optionalCountingWindow[optionalMemberIndexMap.get(key)]++;
                }
            });

            if (allRequiredParticipantsAvailable(runningIntervalCount, requiredCountingWindow)) {
                mostParticipantStartIndex = 0;
                mostParticipantCount = (int) (requiredMemberAvailabilityMap.size()
                    + Arrays.stream(optionalCountingWindow)
                    .filter(count -> count == runningIntervalCount).count());
            }
        }

        for (int i = runningIntervalCount; i < intervalCount; i++) {
            int finalI = i;
            requiredMemberAvailabilityMap.forEach((key, value) -> {
                if (value[finalI - runningIntervalCount] != IntervalStatus.UNAVAILABLE) {
                    requiredCountingWindow[requiredMemberIndexMap.get(key)]--;
                }
                if (value[finalI] != IntervalStatus.UNAVAILABLE) {
                    requiredCountingWindow[requiredMemberIndexMap.get(key)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((key, value) -> {
                if (value[finalI - runningIntervalCount] != IntervalStatus.UNAVAILABLE) {
                    optionalCountingWindow[optionalMemberIndexMap.get(key)]--;
                }
                if (value[finalI] != IntervalStatus.UNAVAILABLE) {
                    optionalCountingWindow[optionalMemberIndexMap.get(key)]++;
                }
            });

            int optionalCount = (int) Arrays.stream(optionalCountingWindow)
                .filter(count -> count == runningIntervalCount).count();

            if (allRequiredParticipantsAvailable(runningIntervalCount, requiredCountingWindow)
                && mostParticipantCount < requiredMemberAvailabilityMap.size() + optionalCount) {
                mostParticipantStartIndex = i - runningIntervalCount + 1;
                mostParticipantCount = requiredMemberAvailabilityMap.size() + optionalCount;
            }
        }

        if (mostParticipantStartIndex < 0) {
            return null;
        }

        List<Integer> availableMemberIds = new ArrayList<>();
        List<Integer> availableMemberInWorkingHourIds = new ArrayList<>();

        int finalMostParticipantStartIndex = mostParticipantStartIndex;
        requiredMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalMostParticipantStartIndex,
                    finalMostParticipantStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] != IntervalStatus.UNAVAILABLE);
            if (available) {
                availableMemberIds.add(id);
            }
        });
        optionalMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalMostParticipantStartIndex,
                    finalMostParticipantStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] != IntervalStatus.UNAVAILABLE);
            if (available) {
                availableMemberIds.add(id);
            }
        });

        requiredMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalMostParticipantStartIndex,
                    finalMostParticipantStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS);
            if (available) {
                availableMemberInWorkingHourIds.add(id);
            }
        });
        optionalMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalMostParticipantStartIndex,
                    finalMostParticipantStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS);
            if (available) {
                availableMemberInWorkingHourIds.add(id);
            }
        });

        return MeetingRecommendation.builder()
            .recommendType(RecommendType.MOST_PARTICIPANTS)
            .start(startDatetime.plusMinutes(mostParticipantStartIndex * 15L))
            .end(
                startDatetime.plusMinutes((mostParticipantStartIndex + runningIntervalCount) * 15L))
            .startIndex(mostParticipantStartIndex)
            .endIndex(mostParticipantStartIndex + runningIntervalCount - 1)
            .availableMemberIds(availableMemberIds)
            .availableMemberInWorkingHourIds(availableMemberInWorkingHourIds)
            .build();
    }

    /**
     * Sliding-Window 알고리즘을 사용하여 가장 많은 참여자가 가능한 회의 시간을 찾는다.
     */
    public MeetingRecommendation findMostParticipantsInWorkingHoursMeeting(
        Map<Integer, IntervalStatus[]> requiredMemberAvailabilityMap,
        Map<Integer, IntervalStatus[]> optionalMemberAvailabilityMap,
        LocalDateTime startDatetime,
        int runningIntervalCount, int intervalCount) {

        int mostParticipantStartIndex = -1;
        int mostParticipantCount = 0;
        int[] requiredAvailableCountingWindow = new int[requiredMemberAvailabilityMap.size()];
        int[] requiredCountingWindow = new int[requiredMemberAvailabilityMap.size()];
        int[] optionalCountingWindow = new int[optionalMemberAvailabilityMap.size()];

        Map<Integer, Integer> requiredMemberIndexMap = new HashMap<>();
        Map<Integer, Integer> optionalMemberIndexMap = new HashMap<>();

        int requiredIndex = 0;
        for (Integer key : requiredMemberAvailabilityMap.keySet()) {
            requiredMemberIndexMap.put(key, requiredIndex++);
        }

        int optionalIndex = 0;
        for (Integer key : optionalMemberAvailabilityMap.keySet()) {
            optionalMemberIndexMap.put(key, optionalIndex++);
        }

        for (int i = 0; i < runningIntervalCount; i++) {
            int finalI = i;
            requiredMemberAvailabilityMap.forEach((key, value) -> {
                if (value[finalI] != IntervalStatus.UNAVAILABLE) {
                    requiredAvailableCountingWindow[requiredMemberIndexMap.get(key)]++;
                }
                if (value[finalI] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    requiredCountingWindow[requiredMemberIndexMap.get(key)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((key, value) -> {
                if (value[finalI] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    optionalCountingWindow[optionalMemberIndexMap.get(key)]++;
                }
            });

            if (allRequiredParticipantsAvailable(runningIntervalCount,
                requiredAvailableCountingWindow)) {
                mostParticipantStartIndex = 0;
                mostParticipantCount = (int) (requiredMemberAvailabilityMap.size()
                    + Arrays.stream(optionalCountingWindow)
                    .filter(count -> count == runningIntervalCount).count());
            }
        }

        for (int i = runningIntervalCount; i < intervalCount; i++) {
            int finalI = i;
            requiredMemberAvailabilityMap.forEach((key, value) -> {
                if (value[finalI - runningIntervalCount]
                    != IntervalStatus.UNAVAILABLE) {
                    requiredAvailableCountingWindow[requiredMemberIndexMap.get(key)]--;
                }
                if (value[finalI] != IntervalStatus.UNAVAILABLE) {
                    requiredAvailableCountingWindow[requiredMemberIndexMap.get(key)]++;
                }
                if (value[finalI - runningIntervalCount]
                    == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    requiredCountingWindow[requiredMemberIndexMap.get(key)]--;
                }
                if (value[finalI] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    requiredCountingWindow[requiredMemberIndexMap.get(key)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((key, value) -> {
                if (value[finalI - runningIntervalCount]
                    == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    optionalCountingWindow[optionalMemberIndexMap.get(key)]--;
                }
                if (value[finalI] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    optionalCountingWindow[optionalMemberIndexMap.get(key)]++;
                }
            });

            int optionalMemberCount = (int) Arrays.stream(optionalCountingWindow)
                .filter(count -> count == runningIntervalCount).count();

            if (allRequiredParticipantsAvailable(runningIntervalCount,
                requiredAvailableCountingWindow)
                && mostParticipantCount
                < requiredMemberAvailabilityMap.size() + optionalMemberCount) {
                mostParticipantStartIndex = i - runningIntervalCount + 1;
                mostParticipantCount = requiredMemberAvailabilityMap.size() + optionalMemberCount;
            }
        }

        if (mostParticipantStartIndex < 0) {
            return null;
        }

        List<Integer> availableMemberIds = new ArrayList<>();
        List<Integer> availableMemberInWorkingHourIds = new ArrayList<>();

        int finalMostParticipantStartIndex = mostParticipantStartIndex;
        requiredMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalMostParticipantStartIndex,
                    finalMostParticipantStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] != IntervalStatus.UNAVAILABLE);
            if (available) {
                availableMemberIds.add(id);
            }
        });
        optionalMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalMostParticipantStartIndex,
                    finalMostParticipantStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] != IntervalStatus.UNAVAILABLE);
            if (available) {
                availableMemberIds.add(id);
            }
        });

        requiredMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalMostParticipantStartIndex,
                    finalMostParticipantStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS);
            if (available) {
                availableMemberInWorkingHourIds.add(id);
            }
        });
        optionalMemberAvailabilityMap.forEach((id, availability) -> {
            boolean available = IntStream.range(finalMostParticipantStartIndex,
                    finalMostParticipantStartIndex + runningIntervalCount)
                .allMatch(i -> availability[i] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS);
            if (available) {
                availableMemberInWorkingHourIds.add(id);
            }
        });

        return MeetingRecommendation.builder()
            .recommendType(RecommendType.MOST_PARTICIPANTS_IN_WORKING_HOUR)
            .start(startDatetime.plusMinutes(mostParticipantStartIndex * 15L))
            .end(
                startDatetime.plusMinutes((mostParticipantStartIndex + runningIntervalCount) * 15L))
            .startIndex(mostParticipantStartIndex)
            .endIndex(mostParticipantStartIndex + runningIntervalCount - 1)
            .availableMemberIds(availableMemberIds)
            .availableMemberInWorkingHourIds(availableMemberInWorkingHourIds)
            .build();
    }

    private static boolean allRequiredParticipantsAvailable(int runningIntervalCount,
        int[] requiredCountingWindow) {
        return Arrays.stream(requiredCountingWindow)
            .allMatch(count -> count == runningIntervalCount);
    }

    private int calculateAffectedIndex(IntervalIndex startIntervalIdx, ScheduleType type,
        boolean isStart) {
        int affectedIndex = startIntervalIdx.index();
        if (isStart && type.equals(ScheduleType.WORKING) && !startIntervalIdx.onBoundary()) {
            affectedIndex++;
        }
        if (!isStart && (type.equals(ScheduleType.WORKING) || startIntervalIdx.onBoundary())) {
            affectedIndex--;
        }
        return affectedIndex;
    }
}
