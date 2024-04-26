package com.edgescheduler.scheduleservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.edgescheduler.scheduleservice.domain.Schedule;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest.CalculatingMember;
import com.edgescheduler.scheduleservice.repository.ScheduleRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SimpleScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Spy
    private SimpleScheduleMediateService simpleScheduleMediateService;

    @InjectMocks
    private SimpleScheduleService simpleScheduleService;

    @Test
    void calculateAvailabilityTest() {

        setUp();

        List<int[]> expected = List.of(
            new int[]{3, 2, 2, 2},
            new int[]{3, 2, 2, 2},
            new int[]{3, 2, 3, 2},
            new int[]{4, 2, 4, 2},
            new int[]{3, 1, 3, 1},
            new int[]{3, 1, 3, 1},
            new int[]{2, 0, 2, 0},
            new int[]{2, 0, 2, 0},
            new int[]{3, 1, 3, 1},
            new int[]{3, 1, 3, 1},
            new int[]{4, 2, 2, 1},
            new int[]{4, 2, 2, 1}
        );

        var request = CalculateAvailabilityRequest.builder()
            .startDatetime(LocalDateTime.of(2024, 4, 25, 10, 0))
            .endDatetime(LocalDateTime.of(2024, 4, 25, 13, 0))
            .memberList(
                List.of(
                    CalculatingMember.builder()
                        .memberId(1)
                        .isRequired(true)
                        .build(),
                    CalculatingMember.builder()
                        .memberId(2)
                        .isRequired(true)
                        .build(),
                    CalculatingMember.builder()
                        .memberId(3)
                        .isRequired(false)
                        .build(),
                    CalculatingMember.builder()
                        .memberId(4)
                        .isRequired(false)
                        .build()
                )
            ).build();

        var result = simpleScheduleService.calculateAvailability(request);

        assertEquals(4, result.getSchedules().size());

        AtomicInteger i = new AtomicInteger(0);
        result.getTokenizedTimeAvailabilities()
            .forEach(availability -> {
                assertEquals(expected.get(i.get())[0], availability.getAvailableMemberCount());
                assertEquals(expected.get(i.get())[1],
                    availability.getAvailableRequiredMemberCount());
                assertEquals(expected.get(i.get())[2],
                    availability.getAvailableMemberInWorkingHourCount());
                assertEquals(expected.get(i.getAndIncrement())[3],
                    availability.getAvailableRequiredMemberInWorkingHourCount());
            });
    }

    private void setUp() {

        Instant startDatetime = Instant.parse("2024-04-25T10:00:00Z");
        Instant endDatetime = Instant.parse("2024-04-25T13:00:00Z");

        when(
            scheduleRepository.findAcceptedSchedulesByAttendeeIdAndEndDatetimeBeforeAndStartDatetimeAfter(
                1, startDatetime, endDatetime
            )).thenReturn(
            List.of(
                Schedule.builder()
                    .id(1L)
                    .name("schedule1")
                    .type(ScheduleType.WORKING)
                    .startDatetime(Instant.parse("2024-04-25T10:00:00Z"))
                    .endDatetime(Instant.parse("2024-04-25T13:00:00Z"))
                    .isPublic(true)
                    .build(),
                Schedule.builder()
                    .id(2L)
                    .name("schedule2")
                    .type(ScheduleType.MEETING)
                    .startDatetime(Instant.parse("2024-04-25T11:00:00Z"))
                    .endDatetime(Instant.parse("2024-04-25T12:00:00Z"))
                    .isPublic(true)
                    .build()
            )
        );
        when(
            scheduleRepository.findAcceptedSchedulesByAttendeeIdAndEndDatetimeBeforeAndStartDatetimeAfter(
                2, startDatetime, endDatetime
            )).thenReturn(
            List.of(
                Schedule.builder()
                    .id(3L)
                    .name("schedule3")
                    .type(ScheduleType.WORKING)
                    .startDatetime(Instant.parse("2024-04-25T09:00:00Z"))
                    .endDatetime(Instant.parse("2024-04-25T12:00:00Z"))
                    .isPublic(true)
                    .build(),
                Schedule.builder()
                    .id(4L)
                    .name("schedule4")
                    .type(ScheduleType.PERSONAL)
                    .startDatetime(Instant.parse("2024-04-25T11:30:00Z"))
                    .endDatetime(Instant.parse("2024-04-25T12:30:00Z"))
                    .isPublic(true)
                    .build()
            )
        );
        when(
            scheduleRepository.findAcceptedSchedulesByAttendeeIdAndEndDatetimeBeforeAndStartDatetimeAfter(
                3, startDatetime, endDatetime
            )).thenReturn(
            List.of(
                Schedule.builder()
                    .id(5L)
                    .name("schedule5")
                    .type(ScheduleType.WORKING)
                    .startDatetime(Instant.parse("2024-04-25T10:30:00Z"))
                    .endDatetime(Instant.parse("2024-04-25T13:00:00Z"))
                    .isPublic(true)
                    .build()
            )
        );
        when(
            scheduleRepository.findAcceptedSchedulesByAttendeeIdAndEndDatetimeBeforeAndStartDatetimeAfter(
                4, startDatetime, endDatetime
            )).thenReturn(
            List.of(
                Schedule.builder()
                    .id(6L)
                    .name("schedule6")
                    .type(ScheduleType.WORKING)
                    .startDatetime(Instant.parse("2024-04-25T10:00:00Z"))
                    .endDatetime(Instant.parse("2024-04-25T12:30:00Z"))
                    .isPublic(true)
                    .build(),
                Schedule.builder()
                    .id(7L)
                    .name("schedule7")
                    .type(ScheduleType.PERSONAL)
                    .startDatetime(Instant.parse("2024-04-25T10:00:00Z"))
                    .endDatetime(Instant.parse("2024-04-25T10:45:00Z"))
                    .isPublic(true)
                    .build()
            )
        );
    }
}