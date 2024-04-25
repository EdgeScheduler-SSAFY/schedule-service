package com.edgescheduler.scheduleservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest.CalculatingMember;
import com.edgescheduler.scheduleservice.repository.SimpleScheduleRepository;
import com.edgescheduler.scheduleservice.vo.ScheduleVO;
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
    private SimpleScheduleRepository scheduleRepository;

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

        when(scheduleRepository.findByMemberIdAndEndDatetimeBeforeAndStartDatetimeAfter(
            1, startDatetime, endDatetime
        )).thenReturn(
            List.of(
                new ScheduleVO(
                    1L,
                    "schedule1",
                    ScheduleType.WORKING,
                    Instant.parse("2024-04-25T10:00:00Z"),
                    Instant.parse("2024-04-25T13:00:00Z"),
                    true
                ),
                new ScheduleVO(
                    2L,
                    "schedule2",
                    ScheduleType.MEETING,
                    Instant.parse("2024-04-25T11:00:00Z"),
                    Instant.parse("2024-04-25T12:00:00Z"),
                    true
                )
            )
        );
        when(scheduleRepository.findByMemberIdAndEndDatetimeBeforeAndStartDatetimeAfter(
            2, startDatetime, endDatetime
        )).thenReturn(
            List.of(
                new ScheduleVO(
                    3L,
                    "schedule3",
                    ScheduleType.WORKING,
                    Instant.parse("2024-04-25T09:00:00Z"),
                    Instant.parse("2024-04-25T12:00:00Z"),
                    true
                ),
                new ScheduleVO(
                    4L,
                    "schedule4",
                    ScheduleType.PERSONAL,
                    Instant.parse("2024-04-25T11:30:00Z"),
                    Instant.parse("2024-04-25T12:30:00Z"),
                    true
                )
            )
        );
        when(scheduleRepository.findByMemberIdAndEndDatetimeBeforeAndStartDatetimeAfter(
            3, startDatetime, endDatetime
        )).thenReturn(
            List.of(
                new ScheduleVO(
                    5L,
                    "schedule1",
                    ScheduleType.WORKING,
                    Instant.parse("2024-04-25T10:30:00Z"),
                    Instant.parse("2024-04-25T13:00:00Z"),
                    true
                )
            )
        );
        when(scheduleRepository.findByMemberIdAndEndDatetimeBeforeAndStartDatetimeAfter(
            4, startDatetime, endDatetime
        )).thenReturn(
            List.of(
                new ScheduleVO(
                    6L,
                    "schedule6",
                    ScheduleType.WORKING,
                    Instant.parse("2024-04-25T10:00:00Z"),
                    Instant.parse("2024-04-25T12:30:00Z"),
                    true
                ),
                new ScheduleVO(
                    2L,
                    "schedule2",
                    ScheduleType.PERSONAL,
                    Instant.parse("2024-04-25T10:00:00Z"),
                    Instant.parse("2024-04-25T10:45:00Z"),
                    true
                )
            )
        );
    }
}