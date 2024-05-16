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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleMediateService {

    private final ScheduleService scheduleService;
    private final MemberTimezoneRepository memberTimezoneRepository;

    public CalculateAvailabilityWithProposalResponse calculateAvailableMembersWithProposedSchedule(
        CalculateAvailabilityWithProposalRequest calculateAvailabilityWithProposalRequest) {

        log.info("request: {}", calculateAvailabilityWithProposalRequest);
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
                .allMatch(this::isAvailable)) {
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
            schedulesAndAvailability.setTzOffset(
                zoneId.getRules().getOffset(zonedStart).toString());
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

        List<MeetingRecommendation> fastestMeetings = findFastestMeeting(
            requiredMemberSchedulesAndAvailabilityMap,
            calculateAvailabilityRequest.getRunningTime() / 15,
            expandedIntervalCount,
            offset);

        List<MeetingRecommendation> mostParticipantsMeetings = findMostParticipantsMeeting(
            requiredMemberSchedulesAndAvailabilityMap,
            optionalMemberSchedulesAndAvailabilityMap,
            calculateAvailabilityRequest.getRunningTime() / 15,
            expandedIntervalCount,
            offset);

        List<MeetingRecommendation> mostParticipantsInWorkingHourMeetings = findMostParticipantsInWorkingHoursMeeting(
            requiredMemberSchedulesAndAvailabilityMap,
            optionalMemberSchedulesAndAvailabilityMap,
            calculateAvailabilityRequest.getRunningTime() / 15,
            expandedIntervalCount,
            offset);

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
    public List<MeetingRecommendation> findFastestMeeting(
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberAvailabilityMap,
        int runningIntervalCount, int intervalCount, int offset) {

        int recommendCount = 0;
        List<MeetingRecommendation> recommendList = new ArrayList<>();

        int availableCount = 0;

        for (int i = 0; i < intervalCount; i++) {
            int finalI = i;
            if (requiredMemberAvailabilityMap.values().stream()
                .allMatch(sa -> isAvailable(sa.getAvailability()[finalI]))) {
                availableCount++;
            } else {
                availableCount = 0;
            }

            if (availableCount == runningIntervalCount) {
                recommendList.add(MeetingRecommendation.builder()
                    .recommendType(RecommendType.FASTEST)
                    .startIndexInclusive(i - runningIntervalCount + 1 + offset)
                    .endIndexExclusive(i + 1 + offset)
                    .build());
                recommendCount++;
                availableCount--;
            }
            if (recommendCount == 3) {
                break;
            }
        }

        return recommendList;
    }

    /**
     * Sliding-Window 알고리즘을 사용하여 가장 많은 참여자가 가능한 회의 시간을 찾는다.
     */
    public List<MeetingRecommendation> findMostParticipantsMeeting(
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberAvailabilityMap,
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberAvailabilityMap,
        int runningIntervalCount, int intervalCount, int offset) {

        int[] requiredCountingWindow = new int[requiredMemberAvailabilityMap.size()];
        int[] optionalCountingWindow = new int[optionalMemberAvailabilityMap.size()];

        Queue<RecommendFactor> recommendQueue = new PriorityQueue<>();
        List<MeetingRecommendation> recommendList = new ArrayList<>();

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
                if (isAvailable(sa.getAvailability()[finalI])) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((id, sa) -> {
                if (isAvailable(sa.getAvailability()[finalI])) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]++;
                }
            });

            if (allRequiredParticipantsAvailable(runningIntervalCount, requiredCountingWindow)) {
                int participantsCount = (int) (requiredMemberAvailabilityMap.size()
                    + Arrays.stream(optionalCountingWindow)
                    .filter(count -> count == runningIntervalCount).count());
                recommendQueue.add(new RecommendFactor(0, participantsCount));
            }
        }

        for (int i = runningIntervalCount; i < intervalCount; i++) {
            int finalI = i;
            requiredMemberAvailabilityMap.forEach((id, sa) -> {
                if (isAvailable(sa.getAvailability()[finalI - runningIntervalCount])) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]--;
                }
                if (isAvailable(sa.getAvailability()[finalI])) {
                    requiredCountingWindow[requiredMemberIndexMap.get(id)]++;
                }
            });
            optionalMemberAvailabilityMap.forEach((id, sa) -> {
                if (isAvailable(sa.getAvailability()[finalI - runningIntervalCount])) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]--;
                }
                if (isAvailable(sa.getAvailability()[finalI])) {
                    optionalCountingWindow[optionalMemberIndexMap.get(id)]++;
                }
            });

            int optionalCount = (int) Arrays.stream(optionalCountingWindow)
                .filter(count -> count == runningIntervalCount).count();

            if (allRequiredParticipantsAvailable(runningIntervalCount, requiredCountingWindow)) {
                int participantsCount = requiredMemberAvailabilityMap.size() + optionalCount;
                if (recommendQueue.size() > 3) {
                    recommendQueue.poll();
                }
                recommendQueue.add(
                    new RecommendFactor(i - runningIntervalCount + 1, participantsCount));
            }
        }

        while (recommendQueue.size() > 3) {
            recommendQueue.poll();
        }

        for (int i = 0; i < 3 && !recommendQueue.isEmpty(); i++) {
            RecommendFactor rf = recommendQueue.poll();
            recommendList.add(MeetingRecommendation.builder()
                .recommendType(RecommendType.MOST_PARTICIPANTS)
                .startIndexInclusive(rf.getStartIndex() + offset)
                .endIndexExclusive(rf.getStartIndex() + runningIntervalCount + offset)
                .build());
        }

        Collections.reverse(recommendList);
        return recommendList;
    }

    /**
     * Sliding-Window 알고리즘을 사용하여 가장 많은 참여자가 가능한 회의 시간을 찾는다.
     */
    public List<MeetingRecommendation> findMostParticipantsInWorkingHoursMeeting(
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberAvailabilityMap,
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberAvailabilityMap,
        int runningIntervalCount, int intervalCount, int offset) {

        Queue<RecommendFactor> recommendQueue = new PriorityQueue<>();
        List<MeetingRecommendation> recommendList = new ArrayList<>();
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
                if (isAvailable(sa.getAvailability()[finalI])) {
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
                int participantsInWorkingHourCount =
                    (int) (Arrays.stream(requiredCountingWindow)
                        .filter(count -> count == runningIntervalCount).count()
                        + Arrays.stream(optionalCountingWindow)
                        .filter(count -> count == runningIntervalCount).count());
                recommendQueue.add(new RecommendFactor(0, participantsInWorkingHourCount));
            }
        }

        for (int i = runningIntervalCount; i < intervalCount; i++) {
            int finalI = i;
            requiredMemberAvailabilityMap.forEach((id, sa) -> {
                if (isAvailable(sa.getAvailability()[finalI - runningIntervalCount])) {
                    requiredAvailableCountingWindow[requiredMemberIndexMap.get(id)]--;
                }
                if (isAvailable(sa.getAvailability()[finalI])) {
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
                requiredAvailableCountingWindow)) {
                int participantsInWorkingHourCount =
                    requiredMemberCount + optionalMemberCount;
                if (recommendQueue.size() > 3) {
                    recommendQueue.poll();
                }
                recommendQueue.add(
                    new RecommendFactor(i - runningIntervalCount + 1,
                        participantsInWorkingHourCount));
            }
        }

        while (recommendQueue.size() > 3) {
            recommendQueue.poll();
        }

        for (int i = 0; i < 3 && !recommendQueue.isEmpty(); i++) {
            RecommendFactor rf = recommendQueue.poll();
            recommendList.add(MeetingRecommendation.builder()
                .recommendType(RecommendType.MOST_PARTICIPANTS_IN_WORKING_HOUR)
                .startIndexInclusive(rf.getStartIndex() + offset)
                .endIndexExclusive(rf.getStartIndex() + runningIntervalCount + offset)
                .build());
        }

        Collections.reverse(recommendList);
        return recommendList;
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
                    .name(schedule.getIsPublic() ? schedule.getName() : "Private")
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

    private boolean isAvailable(IntervalStatus status) {
        return status == IntervalStatus.AVAILABLE
            || status == IntervalStatus.AVAILABLE_IN_WORKING_HOURS;
    }

    @Getter
    @AllArgsConstructor
    private static class RecommendFactor implements Comparable<RecommendFactor> {

        private final int startIndex;
        private final int count;

        @Override
        public int compareTo(RecommendFactor o) { // count가 클수록, startIndex가 작을수록 우선순위가 높다
            int countCompare = Integer.compare(count, o.count);
            if (countCompare != 0) {
                return countCompare;
            }
            return Integer.compare(o.startIndex, startIndex);
        }
    }
}
