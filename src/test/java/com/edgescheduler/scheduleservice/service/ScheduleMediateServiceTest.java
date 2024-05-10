package com.edgescheduler.scheduleservice.service;

import static com.edgescheduler.scheduleservice.vo.IntervalStatus.AVAILABLE;
import static com.edgescheduler.scheduleservice.vo.IntervalStatus.AVAILABLE_IN_WORKING_HOURS;
import static com.edgescheduler.scheduleservice.vo.IntervalStatus.UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse.IndividualSchedulesAndAvailability;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse.ScheduleEntry;
import com.edgescheduler.scheduleservice.dto.response.MeetingRecommendation;
import com.edgescheduler.scheduleservice.dto.response.MeetingRecommendation.RecommendType;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse.IndividualSchedule;
import com.edgescheduler.scheduleservice.vo.IntervalStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    @DisplayName("특정 기간 내 가용 시간 배열 생성")
    @Test
    void getAvailabilityWithinPeriodTest() {

        int intervalCount = 17;
        LocalDateTime start = LocalDateTime.of(2024, 5, 8, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 5, 8, 14, 15);

        List<IndividualSchedule> schedules = List.of(
            IndividualSchedule.builder()
                .type(ScheduleType.PERSONAL)
                .startDatetime(LocalDateTime.of(2024, 5, 8, 9, 0))
                .endDatetime(LocalDateTime.of(2024, 5, 8, 10, 35))
                .scheduleId(1L)
                .name("test1")
                .isPublic(true)
                .color(1)
                .organizerId(10)
                .build(),
            IndividualSchedule.builder()
                .type(ScheduleType.WORKING)
                .startDatetime(LocalDateTime.of(2024, 5, 8, 11, 10))
                .endDatetime(LocalDateTime.of(2024, 5, 8, 13, 5))
                .scheduleId(2L)
                .name("test2")
                .isPublic(true)
                .color(1)
                .organizerId(10)
                .build(),
            IndividualSchedule.builder()
                .type(ScheduleType.MEETING)
                .startDatetime(LocalDateTime.of(2024, 5, 8, 13, 55))
                .endDatetime(LocalDateTime.of(2024, 5, 8, 15, 0))
                .scheduleId(3L)
                .name("test3")
                .isPublic(true)
                .color(1)
                .organizerId(10)
                .build()
        );

        IntervalStatus[] expected = new IntervalStatus[]{
            UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE,
            AVAILABLE, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
            AVAILABLE_IN_WORKING_HOURS,
            AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
            AVAILABLE_IN_WORKING_HOURS,
            AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE,
            UNAVAILABLE
        };

        IntervalStatus[] availability = scheduleMediateService.getAvailabilityWithinPeriod(
            intervalCount,
            schedules,
            start,
            end
        );

        assertArrayEquals(expected, availability);
    }

    @DisplayName("특정 기간 내 일정 목록 & 가용 시간 배열 생성")
    @Test
    void getSchedulesAndAvailabilityWithinPeriodTest() {

        int intervalCount = 17;
        LocalDateTime start = LocalDateTime.of(2024, 5, 8, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 5, 8, 14, 15);

        List<IndividualSchedule> schedules = List.of(
            IndividualSchedule.builder()
                .type(ScheduleType.PERSONAL)
                .startDatetime(LocalDateTime.of(2024, 5, 8, 9, 0))
                .endDatetime(LocalDateTime.of(2024, 5, 8, 10, 35))
                .scheduleId(1L)
                .name("test1")
                .isPublic(true)
                .color(1)
                .organizerId(10)
                .build(),
            IndividualSchedule.builder()
                .type(ScheduleType.WORKING)
                .startDatetime(LocalDateTime.of(2024, 5, 8, 11, 10))
                .endDatetime(LocalDateTime.of(2024, 5, 8, 13, 5))
                .scheduleId(2L)
                .name("test2")
                .isPublic(true)
                .color(1)
                .organizerId(10)
                .build(),
            IndividualSchedule.builder()
                .type(ScheduleType.MEETING)
                .startDatetime(LocalDateTime.of(2024, 5, 8, 13, 55))
                .endDatetime(LocalDateTime.of(2024, 5, 8, 15, 0))
                .scheduleId(3L)
                .name("test3")
                .isPublic(true)
                .color(1)
                .organizerId(10)
                .build()
        );

        List<ScheduleEntry> expectedSchedules = List.of(
            ScheduleEntry.builder()
                .name("test1")
                .startIndexInclusive(0)
                .endIndexExclusive(3)
                .type(ScheduleType.PERSONAL)
                .isPublic(true)
                .build(),
            ScheduleEntry.builder()
                .name("test2")
                .startIndexInclusive(5)
                .endIndexExclusive(12)
                .type(ScheduleType.WORKING)
                .isPublic(true)
                .build(),
            ScheduleEntry.builder()
                .name("test3")
                .startIndexInclusive(15)
                .endIndexExclusive(17)
                .type(ScheduleType.MEETING)
                .isPublic(true)
                .build()
        );

        IntervalStatus[] expectedAvailability = new IntervalStatus[]{
            UNAVAILABLE, UNAVAILABLE, UNAVAILABLE, AVAILABLE,
            AVAILABLE, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
            AVAILABLE_IN_WORKING_HOURS,
            AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS, AVAILABLE_IN_WORKING_HOURS,
            AVAILABLE_IN_WORKING_HOURS,
            AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE,
            UNAVAILABLE
        };

        var schedulesAndAvailability = scheduleMediateService.getSchedulesAndAvailabilityWithinPeriod(
            1,
            true,
            intervalCount,
            0,
            schedules,
            start,
            end
        );

        assertIterableEquals(expectedSchedules, schedulesAndAvailability.getSchedules());
        assertArrayEquals(expectedAvailability, schedulesAndAvailability.getAvailability());
    }

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
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE,
                UNAVAILABLE});

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
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberSaMap =
            requiredMemberAvailabilityMap.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> IndividualSchedulesAndAvailability.builder()
                        .memberId(e.getKey())
                        .isRequired(true)
                        .availability(e.getValue())
                        .build()
                ));
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberSaMap =
            optionalMemberAvailabilityMap.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> IndividualSchedulesAndAvailability.builder()
                        .memberId(e.getKey())
                        .isRequired(false)
                        .availability(e.getValue())
                        .build()
                ));
        MeetingRecommendation recommendation = scheduleMediateService.findFastestMeeting(
            requiredMemberSaMap, optionalMemberSaMap, 3, 17);

        log.info("recommendation: {}", recommendation);

        assertEquals(RecommendType.FASTEST, recommendation.getRecommendType());
        assertEquals(3, recommendation.getStartIndexInclusive());
        assertEquals(6, recommendation.getEndIndexExclusive());
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
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE,
                UNAVAILABLE});

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
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberSaMap =
            requiredMemberAvailabilityMap.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> IndividualSchedulesAndAvailability.builder()
                        .memberId(e.getKey())
                        .isRequired(true)
                        .availability(e.getValue())
                        .build()
                ));
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberSaMap =
            optionalMemberAvailabilityMap.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> IndividualSchedulesAndAvailability.builder()
                        .memberId(e.getKey())
                        .isRequired(false)
                        .availability(e.getValue())
                        .build()
                ));
        MeetingRecommendation recommendation = scheduleMediateService.findMostParticipantsMeeting(
            requiredMemberSaMap, optionalMemberSaMap, 3, 17);

        log.info("recommendation: {}", recommendation);
        assertEquals(RecommendType.MOST_PARTICIPANTS, recommendation.getRecommendType());
        assertEquals(9, recommendation.getStartIndexInclusive());
        assertEquals(12, recommendation.getEndIndexExclusive());
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
                AVAILABLE_IN_WORKING_HOURS, AVAILABLE, AVAILABLE, AVAILABLE, UNAVAILABLE,
                UNAVAILABLE});

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
        Map<Integer, IndividualSchedulesAndAvailability> requiredMemberSaMap =
            requiredMemberAvailabilityMap.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> IndividualSchedulesAndAvailability.builder()
                        .memberId(e.getKey())
                        .isRequired(true)
                        .availability(e.getValue())
                        .build()
                ));
        Map<Integer, IndividualSchedulesAndAvailability> optionalMemberSaMap =
            optionalMemberAvailabilityMap.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> IndividualSchedulesAndAvailability.builder()
                        .memberId(e.getKey())
                        .isRequired(false)
                        .availability(e.getValue())
                        .build()
                ));
        MeetingRecommendation recommendation = scheduleMediateService.findMostParticipantsInWorkingHoursMeeting(
            requiredMemberSaMap, optionalMemberSaMap, 3, 17);

        log.info("recommendation: {}", recommendation);
        assertEquals(RecommendType.MOST_PARTICIPANTS_IN_WORKING_HOUR,
            recommendation.getRecommendType());
        assertEquals(9, recommendation.getStartIndexInclusive());
        assertEquals(12, recommendation.getEndIndexExclusive());
    }
}
