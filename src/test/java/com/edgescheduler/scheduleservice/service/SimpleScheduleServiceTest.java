package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest.CalculatingMember;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SimpleScheduleServiceTest {

    @Test
    void calculateAvailabilityTest() {
        var request = CalculateAvailabilityRequest.builder()
            .startDatetime(LocalDateTime.of(2024, 4, 25, 10, 0))
            .endDatetime(LocalDateTime.of(2024, 4, 25, 13, 0)).organizerId(1).runningTime(60)
            .memberList(List.of(CalculatingMember.builder().memberId(1).isRequired(true).build(),
                CalculatingMember.builder().memberId(2).isRequired(true).build(),
                CalculatingMember.builder().memberId(3).isRequired(false).build(),
                CalculatingMember.builder().memberId(4).isRequired(false).build())).build();

//        List<int[]> expected = List.of(
//            new int[]{3, 2, 2, 2},
//            new int[]{3, 2, 2, 2},
//            new int[]{3, 2, 3, 2},
//            new int[]{4, 2, 4, 2},
//            new int[]{3, 1, 3, 1},
//            new int[]{3, 1, 3, 1},
//            new int[]{2, 0, 2, 0},
//            new int[]{2, 0, 2, 0},
//            new int[]{3, 1, 3, 1},
//            new int[]{3, 1, 3, 1},
//            new int[]{4, 2, 2, 1},
//            new int[]{4, 2, 2, 1}
//        );
//
//        var request = CalculateAvailabilityRequest.builder()
//            .startDatetime(LocalDateTime.of(2024, 4, 25, 10, 0))
//            .endDatetime(LocalDateTime.of(2024, 4, 25, 13, 0))
//            .memberList(
//                List.of(
//                    CalculatingMember.builder()
//                        .memberId(1)
//                        .isRequired(true)
//                        .build(),
//                    CalculatingMember.builder()
//                        .memberId(2)
//                        .isRequired(true)
//                        .build(),
//                    CalculatingMember.builder()
//                        .memberId(3)
//                        .isRequired(false)
//                        .build(),
//                    CalculatingMember.builder()
//                        .memberId(4)
//                        .isRequired(false)
//                        .build()
//                )
//            ).build();
//
//        var result = simpleScheduleService.calculateAvailability(request);
//
//        assertEquals(4, result.getSchedules().size());
//
//        AtomicInteger i = new AtomicInteger(0);
//        result.getTokenizedTimeAvailabilities()
//            .forEach(availability -> {
//                assertEquals(expected.get(i.get())[0], availability.getAvailableMemberCount());
//                assertEquals(expected.get(i.get())[1],
//                    availability.getAvailableRequiredMemberCount());
//                assertEquals(expected.get(i.get())[2],
//                    availability.getAvailableMemberInWorkingHourCount());
//                assertEquals(expected.get(i.getAndIncrement())[3],
//                    availability.getAvailableRequiredMemberInWorkingHourCount());
//            });
    }

//    @BeforeEach
//    void setUp() {
//
//        when(memberTimezoneRepository.findById(anyInt()))
//            .thenReturn(
//                Optional.of(
//                    MemberTimezone.builder()
//                        .id(1)
//                        .zoneId(ZoneOffset.UTC.getId())
//                        .build()
//                )
//            );
//
//        LocalDateTime startDatetime = LocalDateTime.parse("2024-04-25T10:00:00");
//        LocalDateTime endDatetime = LocalDateTime.parse("2024-04-25T13:00:00");
//
//        when(
//            simpleScheduleService.getScheduleByPeriod(1, startDatetime, endDatetime)
//        ).thenReturn(
//            ScheduleListReadResponse.builder()
//                .scheduleList(
//                    List.of(
//                        IndividualSchedule.builder()
//                            .scheduleId(1L)
//                            .name("schedule1")
//                            .type(ScheduleType.WORKING)
//                            .startDatetime(LocalDateTime.parse("2024-04-25T10:00:00"))
//                            .endDatetime(LocalDateTime.parse("2024-04-25T13:00:00"))
//                            .isPublic(true)
//                            .build(),
//                        IndividualSchedule.builder()
//                            .scheduleId(2L)
//                            .name("schedule2")
//                            .type(ScheduleType.MEETING)
//                            .startDatetime(LocalDateTime.parse("2024-04-25T11:00:00"))
//                            .endDatetime(LocalDateTime.parse("2024-04-25T12:00:00"))
//                            .isPublic(true)
//                            .build()
//                    )
//                )
//                .build()
//        );
//        when(
//            simpleScheduleService.getScheduleByPeriod(2, startDatetime, endDatetime)
//        ).thenReturn(
//            ScheduleListReadResponse.builder()
//                .scheduleList(
//                    List.of(
//                        IndividualSchedule.builder()
//                            .scheduleId(3L)
//                            .name("schedule3")
//                            .type(ScheduleType.WORKING)
//                            .startDatetime(LocalDateTime.parse("2024-04-25T09:00:00"))
//                            .endDatetime(LocalDateTime.parse("2024-04-25T12:00:00"))
//                            .isPublic(true)
//                            .build(),
//                        IndividualSchedule.builder()
//                            .scheduleId(4L)
//                            .name("schedule4")
//                            .type(ScheduleType.PERSONAL)
//                            .startDatetime(LocalDateTime.parse("2024-04-25T11:30:00"))
//                            .endDatetime(LocalDateTime.parse("2024-04-25T12:30:00"))
//                            .isPublic(true)
//                            .build()
//                    )
//                ).build()
//        );
//        when(
//            simpleScheduleService.getScheduleByPeriod(3, startDatetime, endDatetime)
//        ).thenReturn(
//            ScheduleListReadResponse.builder()
//                .scheduleList(
//                    List.of(
//                        IndividualSchedule.builder()
//                            .scheduleId(5L)
//                            .name("schedule5")
//                            .type(ScheduleType.WORKING)
//                            .startDatetime(LocalDateTime.parse("2024-04-25T10:30:00"))
//                            .endDatetime(LocalDateTime.parse("2024-04-25T13:00:00"))
//                            .isPublic(true)
//                            .build()
//                    )
//                ).build()
//        );
//        when(
//            simpleScheduleService.getScheduleByPeriod(4, startDatetime, endDatetime)
//        ).thenReturn(
//            ScheduleListReadResponse.builder()
//                .scheduleList(
//                    List.of(
//                        IndividualSchedule.builder()
//                            .scheduleId(6L)
//                            .name("schedule6")
//                            .type(ScheduleType.WORKING)
//                            .startDatetime(LocalDateTime.parse("2024-04-25T10:00:00"))
//                            .endDatetime(LocalDateTime.parse("2024-04-25T12:30:00"))
//                            .isPublic(true)
//                            .build(),
//                        IndividualSchedule.builder()
//                            .scheduleId(7L)
//                            .name("schedule7")
//                            .type(ScheduleType.PERSONAL)
//                            .startDatetime(LocalDateTime.parse("2024-04-25T10:00:00"))
//                            .endDatetime(LocalDateTime.parse("2024-04-25T10:45:00"))
//                            .isPublic(true)
//                            .build()
//                    )
//                ).build()
//        );
//    }
}