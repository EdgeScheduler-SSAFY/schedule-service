package com.edgescheduler.scheduleservice.service;

import static org.junit.jupiter.api.Assertions.*;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.vo.ScheduleVO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleMediateServiceTest {

  @Spy
  private SimpleScheduleMediateService scheduleMediateService;

  private static List<ScheduleVO> schedules;

  @BeforeAll
  static void setUpSchedules() {
    schedules =
        List.of(
            new ScheduleVO(
                1L,
                ScheduleType.PERSONAL,
                Instant.parse("2024-04-09T23:30:00Z"),
                Instant.parse("2024-04-10T05:30:00Z")),
            new ScheduleVO(
                2L,
                ScheduleType.PERSONAL,
                Instant.parse("2024-04-10T07:10:00Z"),
                Instant.parse("2024-04-10T08:00:00Z")),
            new ScheduleVO(
                3L,
                ScheduleType.MEETING,
                Instant.parse("2024-04-10T11:00:00Z"),
                Instant.parse("2024-04-10T13:30:00Z")),
            new ScheduleVO(
                4L,
                ScheduleType.PERSONAL,
                Instant.parse("2024-04-10T19:30:00Z"),
                Instant.parse("2024-04-10T20:30:00Z")),
            new ScheduleVO(
                5L,
                ScheduleType.PERSONAL,
                Instant.parse("2024-04-10T23:30:00Z"),
                Instant.parse("2024-04-11T05:30:00Z")),
            new ScheduleVO(
                6L,
                ScheduleType.WORKING,
                Instant.parse("2024-04-10T09:00:00Z"),
                Instant.parse("2024-04-10T18:00:00Z")));
  }

  @Test
  void isAvailableWithOtherSchedule() {

    List<Instant> timeTokens = new ArrayList<>();
    Instant startTime = Instant.parse("2024-04-10T00:00:00Z");

    for (int i = 0; i < 96; i++) {
      timeTokens.add(startTime.plusSeconds(i * 900));
    }

    Boolean[] expected = {
        false, false, false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false, true, true,
        true, true, true, true, false, false, false, false, true, true, true, true,
        true, true, true, true, true, true, true, true, false, false, false, false,
        false, false, false, false, false, false, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, false, false, false, false, true, true,
        true, true, true, true, true, true, true, true, true, true, false, false
    };

    var availabilities =
        timeTokens.stream()
            .map(
                time ->
                    scheduleMediateService.isAvailableWithOtherSchedule(
                        time, time.plusSeconds(900), schedules))
            .toArray();

    assertArrayEquals(
        expected,
        availabilities
    );
  }
}
