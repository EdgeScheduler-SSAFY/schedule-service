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
    /**
     *  2024-04-10 schedules
     *  00:00 ~ 06:00 : PERSONAL
     *  07:10 ~ 08:00 : PERSONAL
     *  09:00 ~ 18:00 : WORKING     ** available time
     *  11:00 ~ 13:30 : MEETING
     *  19:30 ~ 20:30 : PERSONAL
     *  23:30 ~ 24:00 : PERSONAL
     */
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
        false, false, false, false,   // 00:00 ~ 01:00
        false, false, false, false,   // 01:00 ~ 02:00
        false, false, false, false,   // 02:00 ~ 03:00
        false, false, false, false,   // 03:00 ~ 04:00
        false, false, false, false,   // 04:00 ~ 05:00
        false, false, true, true,     // 05:00 ~ 06:00
        true, true, true, true,       // 06:00 ~ 07:00
        false, false, false, false,   // 07:00 ~ 08:00
        true, true, true, true,       // 08:00 ~ 09:00
        true, true, true, true,       // 09:00 ~ 10:00
        true, true, true, true,       // 10:00 ~ 11:00
        false, false, false, false,   // 11:00 ~ 12:00
        false, false, false, false,   // 12:00 ~ 13:00
        false, false, true, true,     // 13:00 ~ 14:00
        true, true, true, true,       // 14:00 ~ 15:00
        true, true, true, true,       // 15:00 ~ 16:00
        true, true, true, true,       // 16:00 ~ 17:00
        true, true, true, true,       // 17:00 ~ 18:00
        true, true, true, true,       // 18:00 ~ 19:00
        true, true, false, false,     // 19:00 ~ 20:00
        false, false, true, true,     // 20:00 ~ 21:00
        true, true, true, true,       // 21:00 ~ 22:00
        true, true, true, true,       // 22:00 ~ 23:00
        true, true, false, false      // 23:00 ~ 24:00
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
