package com.edgescheduler.scheduleservice.service;

import static com.edgescheduler.scheduleservice.vo.IntervalStatus.AVAILABLE;
import static com.edgescheduler.scheduleservice.vo.IntervalStatus.AVAILABLE_IN_WORKING_HOURS;
import static com.edgescheduler.scheduleservice.vo.IntervalStatus.UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.edgescheduler.scheduleservice.service.SimpleScheduleService.MeetingRecommendation;
import com.edgescheduler.scheduleservice.service.SimpleScheduleService.MeetingRecommendation.RecommendType;
import com.edgescheduler.scheduleservice.vo.IntervalStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ScheduleMediateServiceTest {

    @InjectMocks
    private ScheduleMediateServiceImpl scheduleMediateService;

    @DisplayName("가장 빠른 회의 시간 추천")
    @Test
    void findFastestMeetingTest() {

        Map<Integer, IntervalStatus[]> requiredMemberAvailabilityMap = new HashMap<>();
        Map<Integer, IntervalStatus[]> optionalMemberAvailabilityMap = new HashMap<>();

        requiredMemberAvailabilityMap.put(1,
            new IntervalStatus[]{UNAVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                UNAVAILABLE, UNAVAILABLE, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE, UNAVAILABLE, UNAVAILABLE, UNAVAILABLE,
                UNAVAILABLE});
        requiredMemberAvailabilityMap.put(2,
            new IntervalStatus[]{UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE});

        optionalMemberAvailabilityMap.put(3,
            new IntervalStatus[]{AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE});
        optionalMemberAvailabilityMap.put(4,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS});
        optionalMemberAvailabilityMap.put(5,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE,
                UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS});
        optionalMemberAvailabilityMap.put(6,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE});
        List<Integer> availableMemberIds = List.of(1, 2, 3, 4, 5, 6);
        availableMemberIds.stream().toList();
        MeetingRecommendation recommendation = scheduleMediateService.findFastestMeeting(
            requiredMemberAvailabilityMap, optionalMemberAvailabilityMap,
            LocalDateTime.of(2024, 5, 8, 10, 0), 3, 17);

        log.info("recommendation: {}", recommendation);

        assertEquals(RecommendType.FASTEST, recommendation.getRecommendType());
        assertEquals(LocalDateTime.of(2024, 5, 8, 10, 45), recommendation.getStart());
        assertEquals(LocalDateTime.of(2024, 5, 8, 11, 30), recommendation.getEnd());
        assertEquals(3, recommendation.getStartIndex());
        assertIterableEquals(List.of(1, 2, 3, 4, 6), recommendation.getAvailableMemberIds());
        assertIterableEquals(List.of(1, 3, 4), recommendation.getAvailableMemberInWorkingHourIds());
    }

    @DisplayName("참여 가능 인원이 가장 많은 회의 시간 추천")
    @Test
    void findMostParticipantsMeetingTest() {

        Map<Integer, IntervalStatus[]> requiredMemberAvailabilityMap = new HashMap<>();
        Map<Integer, IntervalStatus[]> optionalMemberAvailabilityMap = new HashMap<>();

        requiredMemberAvailabilityMap.put(1,
            new IntervalStatus[]{UNAVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                UNAVAILABLE, UNAVAILABLE, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE, UNAVAILABLE, UNAVAILABLE, UNAVAILABLE,
                UNAVAILABLE});
        requiredMemberAvailabilityMap.put(2,
            new IntervalStatus[]{UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE});

        optionalMemberAvailabilityMap.put(3,
            new IntervalStatus[]{AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE});
        optionalMemberAvailabilityMap.put(4,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS});
        optionalMemberAvailabilityMap.put(5,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE,
                UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS});
        optionalMemberAvailabilityMap.put(6,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE});

        MeetingRecommendation recommendation = scheduleMediateService.findMostParticipantsMeeting(
            requiredMemberAvailabilityMap, optionalMemberAvailabilityMap,
            LocalDateTime.of(2024, 5, 8, 10, 0), 3, 17);

        log.info("recommendation: {}", recommendation);
        assertEquals(RecommendType.MOST_PARTICIPANTS, recommendation.getRecommendType());
        assertEquals(LocalDateTime.of(2024, 5, 8, 12, 15), recommendation.getStart());
        assertEquals(LocalDateTime.of(2024, 5, 8, 13, 0), recommendation.getEnd());
        assertEquals(9, recommendation.getStartIndex());
        assertIterableEquals(List.of(1, 2, 3, 4, 5, 6), recommendation.getAvailableMemberIds());
        assertIterableEquals(List.of(1, 4, 5), recommendation.getAvailableMemberInWorkingHourIds());
    }

    @DisplayName("근무 시간에 참여 가능한 인원이 가장 많은 회의 시간 추천")
    @Test
    void findMostParticipantsInWorkingHoursMeetingTest() {

        Map<Integer, IntervalStatus[]> requiredMemberAvailabilityMap = new HashMap<>();
        Map<Integer, IntervalStatus[]> optionalMemberAvailabilityMap = new HashMap<>();

        requiredMemberAvailabilityMap.put(1,
            new IntervalStatus[]{UNAVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                UNAVAILABLE, UNAVAILABLE, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE, UNAVAILABLE, UNAVAILABLE, UNAVAILABLE,
                UNAVAILABLE});
        requiredMemberAvailabilityMap.put(2,
            new IntervalStatus[]{UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE});

        optionalMemberAvailabilityMap.put(3,
            new IntervalStatus[]{AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE});
        optionalMemberAvailabilityMap.put(4,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS});
        optionalMemberAvailabilityMap.put(5,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE, UNAVAILABLE,
                UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
                AVAILABLE_IN_WORKING_HOURS});
        optionalMemberAvailabilityMap.put(6,
            new IntervalStatus[]{AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE,
                AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE});

        MeetingRecommendation recommendation = scheduleMediateService.findMostParticipantsInWorkingHoursMeeting(
            requiredMemberAvailabilityMap, optionalMemberAvailabilityMap,
            LocalDateTime.of(2024, 5, 8, 10, 0), 3, 17);

        log.info("recommendation: {}", recommendation);
        assertEquals(RecommendType.MOST_PARTICIPANTS_IN_WORKING_HOUR,
            recommendation.getRecommendType());
        assertEquals(LocalDateTime.of(2024, 5, 8, 10, 45), recommendation.getStart());
        assertEquals(LocalDateTime.of(2024, 5, 8, 11, 30), recommendation.getEnd());
        assertEquals(3, recommendation.getStartIndex());
        assertIterableEquals(List.of(1, 2, 3, 4, 6), recommendation.getAvailableMemberIds());
        assertIterableEquals(List.of(1, 3, 4), recommendation.getAvailableMemberInWorkingHourIds());
    }
}
