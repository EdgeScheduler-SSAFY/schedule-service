package com.edgescheduler.scheduleservice.service;

import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.calculateAdjustedIntervalCount;
import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.calculateIntervalCount;
import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.calculateIntervalIndexWithinPeriod;
import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.getExpandedEndOfTheDay;
import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.getMinuteDuration;
import static com.edgescheduler.scheduleservice.util.TimeIntervalUtils.getStartOfTheDay;

import com.edgescheduler.scheduleservice.domain.MemberTimezone;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityWithProposalRequest;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse.IndividualSchedulesAndAvailability;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse.ScheduleEntry;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityWithProposalResponse;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityWithProposalResponse.AvailableMember;
import com.edgescheduler.scheduleservice.dto.response.MeetingRecommendation;
import com.edgescheduler.scheduleservice.dto.response.MeetingRecommendation.RecommendType;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse.IndividualSchedule;
import com.edgescheduler.scheduleservice.exception.ErrorCode;
import com.edgescheduler.scheduleservice.repository.MemberTimezoneRepository;
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
                    .memberName(attendee.getMemberName())
                    .isRequired(attendee.getIsRequired())
                    .build());
            } else {
                unavailableMembers.add(AvailableMember.builder()
                    .memberId(attendee.getMemberId())
                    .memberName(attendee.getMemberName())
                    .isRequired(attendee.getIsRequired())
                    .build());
            }
        });

        return CalculateAvailabilityWithProposalResponse.builder()
            .availableMembers(availableMembers)
            .unavailableMembers(unavailableMembers)
            .build();
    }

    @Override
    public CalculateAvailabilityResponse calculateAvailability(
        CalculateAvailabilityRequest calculateAvailabilityRequest) {

        var startDateTime = calculateAvailabilityRequest.getStartDatetime();
        var endDateTime = calculateAvailabilityRequest.getEndDatetime();
        var expandedStartDateTime = getStartOfTheDay(startDateTime);
        var expandedEndDateTime = getExpandedEndOfTheDay(endDateTime);

        // 주어진 기한 조건을 일단위로 확장한 시작 일시와 끝 일시 사이의 토큰화된 구간의 개수를 계산한다.
        int intervalCount = calculateAdjustedIntervalCount(startDateTime, endDateTime);
        int expandedIntervalCount = calculateIntervalCount(expandedStartDateTime,
            expandedEndDateTime);

        // 가용 시간 배열 확장을 위한 offset 계산
        int offset = getMinuteDuration(expandedStartDateTime, startDateTime) / 15;

        if (intervalCount < calculateAvailabilityRequest.getRunningTime() / 15) {
            throw ErrorCode.INVALID_INTERVAL_COUNT.build();
        }

        MemberTimezone organizerTimezone = memberTimezoneRepository.findById(
                calculateAvailabilityRequest.getOrganizerId())
            .orElseThrow(ErrorCode.TIMEZONE_NOT_FOUND::build);

        // 조정된 시작 일시와 끝 일시를 UTC 표준시로 변환한다
        Instant start = AlterTimeUtils.LocalDateTimeToInstant(
            startDateTime,
            ZoneId.of(organizerTimezone.getZoneId()));
        Instant end = AlterTimeUtils.LocalDateTimeToInstant(
            endDateTime,
            ZoneId.of(organizerTimezone.getZoneId()));

        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberSchedulesAndAvailabilityMap = new HashMap<>();
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberSchedulesAndAvailabilityMap = new HashMap<>();

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
            IndividualSchedulesAndAvailability schedulesAndAvailability = getSchedulesAndAvailabilityWithinPeriod(
                member.getMemberId(), member.getIsRequired(), intervalCount, offset, schedules,
                zonedStart, zonedEnd);
            IntervalStatus[] expandedAvailability = new IntervalStatus[(int) expandedIntervalCount];
            Arrays.fill(expandedAvailability, IntervalStatus.BLOCKED);
            System.arraycopy(schedulesAndAvailability.getAvailability(), 0, expandedAvailability,
                offset, intervalCount);
            schedulesAndAvailability.setAvailability(expandedAvailability);
            if (member.getIsRequired()) {
                requiredMemberSchedulesAndAvailabilityMap.put(member.getMemberId(),
                    schedulesAndAvailability);
            } else {
                optionalMemberSchedulesAndAvailabilityMap.put(member.getMemberId(),
                    schedulesAndAvailability);
            }
        });

        List<IndividualSchedulesAndAvailability> schedulesAndAvailabilities = new ArrayList<>();
        schedulesAndAvailabilities.addAll(requiredMemberSchedulesAndAvailabilityMap.values());
        schedulesAndAvailabilities.addAll(optionalMemberSchedulesAndAvailabilityMap.values());

        List<MeetingRecommendation> fastestMeetings = new ArrayList<>();
        List<MeetingRecommendation> mostParticipantsMeetings = new ArrayList<>();
        List<MeetingRecommendation> mostParticipantsInWorkingHourMeetings = new ArrayList<>();

        MeetingRecommendation fastestMeeting = findFastestMeeting(
            requiredMemberSchedulesAndAvailabilityMap,
            optionalMemberSchedulesAndAvailabilityMap,
            calculateAvailabilityRequest.getRunningTime() / 15,
            expandedIntervalCount);
        if (fastestMeeting != null) {
            fastestMeetings.add(fastestMeeting);
        }

        MeetingRecommendation mostParticipantsMeeting = findMostParticipantsMeeting(
            requiredMemberSchedulesAndAvailabilityMap,
            optionalMemberSchedulesAndAvailabilityMap,
            calculateAvailabilityRequest.getRunningTime() / 15,
            expandedIntervalCount);
        if (mostParticipantsMeeting != null) {
            mostParticipantsMeetings.add(mostParticipantsMeeting);
        }

        MeetingRecommendation mostParticipantsInWorkingHourMeeting = findMostParticipantsInWorkingHoursMeeting(
            requiredMemberSchedulesAndAvailabilityMap,
            optionalMemberSchedulesAndAvailabilityMap,
            calculateAvailabilityRequest.getRunningTime() / 15,
            expandedIntervalCount);
        if (mostParticipantsInWorkingHourMeeting != null) {
            mostParticipantsInWorkingHourMeetings.add(mostParticipantsInWorkingHourMeeting);
        }

        return CalculateAvailabilityResponse.builder()
            .schedulesAndAvailabilities(schedulesAndAvailabilities)
            .fastestMeetings(fastestMeetings)
            .mostParticipantsMeetings(mostParticipantsMeetings)
            .mostParticipantsInWorkingHourMeetings(mostParticipantsInWorkingHourMeetings)
            .build();
    }

    /**
     * 필수 참여자가 모두 참여할 수 있는 가장 빠른 회의 시간을 찾는다.
     */
    public MeetingRecommendation findFastestMeeting(
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberAvailabilityMap,
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberAvailabilityMap,
        int runningIntervalCount, int intervalCount) {

        int fastestStartIndex = -1;
        int availableCount = 0;
        for (int i = 0; i < intervalCount; i++) {
            int finalI = i;
            if (requiredMemberAvailabilityMap.values().stream()
                .allMatch(sa -> sa.getAvailability()[finalI] != IntervalStatus.UNAVAILABLE)) {
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

//        int finalFastestStartIndex = fastestStartIndex;
//        requiredMemberAvailabilityMap.forEach((id, sa) -> {
//            boolean available = IntStream.range(finalFastestStartIndex,
//                    finalFastestStartIndex + runningIntervalCount)
//                .allMatch(i -> sa.getAvailability()[i] != IntervalStatus.UNAVAILABLE);
//            if (available) {
//                availableMemberIds.add(id);
//            }
//        });
//        optionalMemberAvailabilityMap.forEach((id, sa) -> {
//            boolean available = IntStream.range(finalFastestStartIndex,
//                    finalFastestStartIndex + runningIntervalCount)
//                .allMatch(i -> sa.getAvailability()[i] != IntervalStatus.UNAVAILABLE);
//            if (available) {
//                availableMemberIds.add(id);
//            }
//        });
//
//        requiredMemberAvailabilityMap.forEach((id, sa) -> {
//            boolean available = IntStream.range(finalFastestStartIndex,
//                    finalFastestStartIndex + runningIntervalCount)
//                .allMatch(
//                    i -> sa.getAvailability()[i] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS);
//            if (available) {
//                availableMemberInWorkingHourIds.add(id);
//            }
//        });
//        optionalMemberAvailabilityMap.forEach((id, sa) -> {
//            boolean available = IntStream.range(finalFastestStartIndex,
//                    finalFastestStartIndex + runningIntervalCount)
//                .allMatch(
//                    i -> sa.getAvailability()[i] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS);
//            if (available) {
//                availableMemberInWorkingHourIds.add(id);
//            }
//        });

        return MeetingRecommendation.builder()
            .recommendType(RecommendType.FASTEST)
            .startIndexInclusive(fastestStartIndex)
            .endIndexExclusive(fastestStartIndex + runningIntervalCount)
            .build();
    }

    /**
     * Sliding-Window 알고리즘을 사용하여 가장 많은 참여자가 가능한 회의 시간을 찾는다.
     */
    public MeetingRecommendation findMostParticipantsMeeting(
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberAvailabilityMap,
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberAvailabilityMap,
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
            requiredMemberAvailabilityMap.forEach((id, sa) -> {
                if (sa.getAvailability()[finalI] != IntervalStatus.UNAVAILABLE) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((id, sa) -> {
                if (sa.getAvailability()[finalI] != IntervalStatus.UNAVAILABLE) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]++;
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
            requiredMemberAvailabilityMap.forEach((id, sa) -> {
                if (sa.getAvailability()[finalI - runningIntervalCount]
                    != IntervalStatus.UNAVAILABLE) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]--;
                }
                if (sa.getAvailability()[finalI] != IntervalStatus.UNAVAILABLE) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((id, sa) -> {
                if (sa.getAvailability()[finalI - runningIntervalCount]
                    != IntervalStatus.UNAVAILABLE) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]--;
                }
                if (sa.getAvailability()[finalI] != IntervalStatus.UNAVAILABLE) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]++;
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

        return MeetingRecommendation.builder()
            .recommendType(RecommendType.MOST_PARTICIPANTS)
            .startIndexInclusive(mostParticipantStartIndex)
            .endIndexExclusive(mostParticipantStartIndex + runningIntervalCount)
            .build();
    }

    /**
     * Sliding-Window 알고리즘을 사용하여 가장 많은 참여자가 가능한 회의 시간을 찾는다.
     */
    public MeetingRecommendation findMostParticipantsInWorkingHoursMeeting(
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberAvailabilityMap,
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberAvailabilityMap,
        int runningIntervalCount, int intervalCount) {

        int mostParticipantInWorkingHourStartIndex = -1;
        int mostParticipantInWorkingHourCount = 0;
        // 필참 인원은 모두 참여 가능해야 하므로 참여 가능 인원 별도로 카운트
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
            requiredMemberAvailabilityMap.forEach((id, sa) -> {
                if (sa.getAvailability()[finalI] != IntervalStatus.UNAVAILABLE) {
                    requiredAvailableCountingWindow[requiredMemberIndexMap.get(id)]++;
                }
                if (sa.getAvailability()[finalI] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((id, sa) -> {
                if (sa.getAvailability()[finalI] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]++;
                }
            });

            if (allRequiredParticipantsAvailable(runningIntervalCount,
                requiredAvailableCountingWindow)) {
                mostParticipantInWorkingHourStartIndex = 0;
                mostParticipantInWorkingHourCount =
                    (int) (Arrays.stream(requiredCountingWindow)
                        .filter(count -> count == runningIntervalCount).count()
                        + Arrays.stream(optionalCountingWindow)
                        .filter(count -> count == runningIntervalCount).count());
            }
        }

        for (int i = runningIntervalCount; i < intervalCount; i++) {
            int finalI = i;
            requiredMemberAvailabilityMap.forEach((id, sa) -> {
                if (sa.getAvailability()[finalI - runningIntervalCount]
                    != IntervalStatus.UNAVAILABLE) {
                    requiredAvailableCountingWindow[requiredMemberIndexMap.get(id)]--;
                }
                if (sa.getAvailability()[finalI] != IntervalStatus.UNAVAILABLE) {
                    requiredAvailableCountingWindow[requiredMemberIndexMap.get(id)]++;
                }
                if (sa.getAvailability()[finalI - runningIntervalCount]
                    == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]--;
                }
                if (sa.getAvailability()[finalI] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((id, sa) -> {
                if (sa.getAvailability()[finalI - runningIntervalCount]
                    == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]--;
                }
                if (sa.getAvailability()[finalI] == IntervalStatus.AVAILABLE_IN_WORKING_HOURS) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]++;
                }
            });

            int requiredMemberCount = (int) Arrays.stream(requiredCountingWindow)
                .filter(count -> count == runningIntervalCount).count();
            int optionalMemberCount = (int) Arrays.stream(optionalCountingWindow)
                .filter(count -> count == runningIntervalCount).count();

            if (allRequiredParticipantsAvailable(runningIntervalCount,
                requiredAvailableCountingWindow)
                && mostParticipantInWorkingHourCount
                < requiredMemberCount + optionalMemberCount) {
                mostParticipantInWorkingHourStartIndex = i - runningIntervalCount + 1;
                mostParticipantInWorkingHourCount =
                    requiredMemberCount + optionalMemberCount;
            }
        }

        if (mostParticipantInWorkingHourStartIndex < 0) {
            return null;
        }

        return MeetingRecommendation.builder()
            .recommendType(RecommendType.MOST_PARTICIPANTS_IN_WORKING_HOUR)
            .startIndexInclusive(mostParticipantInWorkingHourStartIndex)
            .endIndexExclusive(mostParticipantInWorkingHourStartIndex + runningIntervalCount)
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

    public IndividualSchedulesAndAvailability getSchedulesAndAvailabilityWithinPeriod(
        Integer memberId,
        Boolean isRequired,
        int intervalCount,
        int offset,
        List<IndividualSchedule> schedules,
        LocalDateTime zonedStart, LocalDateTime zonedEnd) {
        List<ScheduleEntry> scheduleEntries = new ArrayList<>();
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
            scheduleEntries.add(
                ScheduleEntry.builder()
                    .name(schedule.getName())
                    .startIndexInclusive(offset + startAffectedIndex)
                    .endIndexExclusive(offset + endAffectedIndex + 1)
                    .type(schedule.getType())
                    .isPublic(schedule.getIsPublic())
                    .build());
            for (int i = startAffectedIndex; i <= endAffectedIndex; i++) {
                if (schedule.getType() != ScheduleType.WORKING) {
                    availability[i] = IntervalStatus.UNAVAILABLE;
                } else if (availability[i] == IntervalStatus.AVAILABLE) {
                    availability[i] = IntervalStatus.AVAILABLE_IN_WORKING_HOURS;
                }
            }
        }
        return IndividualSchedulesAndAvailability.builder()
            .memberId(memberId)
            .isRequired(isRequired)
            .schedules(scheduleEntries)
            .availability(availability)
            .build();
    }

    private static boolean allRequiredParticipantsAvailable(int runningIntervalCount,
        int[] requiredCountingWindow) {
        return Arrays.stream(requiredCountingWindow)
            .allMatch(count -> count == runningIntervalCount);
    }

    private int calculateAffectedIndex(IntervalIndex intervalIndex, ScheduleType type,
        boolean isStart) {
        int affectedIndex = intervalIndex.index();
        if (isStart && type.equals(ScheduleType.WORKING) && !intervalIndex.onBoundary()) {
            affectedIndex++;
        }
        if (!isStart && (type.equals(ScheduleType.WORKING) || intervalIndex.onBoundary())) {
            affectedIndex--;
        }
        return affectedIndex;
    }
}
