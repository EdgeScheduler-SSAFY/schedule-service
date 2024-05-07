package com.edgescheduler.scheduleservice.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.edgescheduler.scheduleservice.domain.Attendee;
import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import com.edgescheduler.scheduleservice.domain.Schedule;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@Slf4j
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class ScheduleRepositoryTest {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @DisplayName("일정 등록")
    @Test
    void save() {
        Schedule schedule = Schedule.builder()
            .organizerId(1)
            .name("일정명")
            .description("일정 내용")
            .type(ScheduleType.valueOf("MEETING"))
            .color(1)
            .startDatetime(Instant.parse("2024-04-24T11:00:00Z"))
            .endDatetime(Instant.parse("2024-04-24T11:00:00Z"))
            .googleCalendarId("googleCalendarId1")
            .isPublic(true)
            .isDeleted(false)
            .build();

        int member1Id = 1;
        int member2Id = 2;
        int member3Id = 3;

        Attendee attendee1 = Attendee.builder()
            .schedule(schedule)
            .memberId(member1Id)
            .isRequired(true)
            .status(AttendeeStatus.valueOf("PENDING"))
            .build();

        Attendee attendee2 = Attendee.builder()
            .schedule(schedule)
            .memberId(member2Id)
            .isRequired(true)
            .status(AttendeeStatus.valueOf("PENDING"))
            .build();

        Attendee attendee3 = Attendee.builder()
            .schedule(schedule)
            .memberId(member3Id)
            .isRequired(true)
            .status(AttendeeStatus.valueOf("PENDING"))
            .build();

        List<Attendee> attendees = List.of(attendee1, attendee2, attendee3);

        schedule.setAttendees(attendees);

        Schedule savedSchedule = scheduleRepository.save(schedule);

        assertAll(
            () -> assertEquals(3, savedSchedule.getAttendees().size()),
            () -> assertEquals(schedule.getName(), savedSchedule.getName()),
            () -> assertEquals(schedule.getDescription(), savedSchedule.getDescription()),
            () -> assertEquals(schedule.getType(), savedSchedule.getType()),
            () -> assertEquals(schedule.getColor(), savedSchedule.getColor()),
            () -> assertEquals(schedule.getStartDatetime(), savedSchedule.getStartDatetime()),
            () -> assertEquals(schedule.getEndDatetime(), savedSchedule.getEndDatetime()),
            () -> assertEquals(schedule.getIsPublic(), savedSchedule.getIsPublic()),
            () -> assertEquals(schedule.getIsDeleted(), savedSchedule.getIsDeleted())
        );
    }

    @DisplayName("일정 상세 조회")
    @Test
    void findByScheduleId() {
        Schedule schedule = Schedule.builder()
            .organizerId(1)
            .name("일정명")
            .description("일정 내용")
            .type(ScheduleType.valueOf("MEETING"))
            .color(1)
            .startDatetime(Instant.parse("2024-04-24T11:00:00Z"))
            .endDatetime(Instant.parse("2024-04-24T11:00:00Z"))
            .googleCalendarId("googleCalendarId1")
            .isPublic(true)
            .isDeleted(false)
            .build();
        Schedule sc = scheduleRepository.save(schedule);

        Long scheduleId = sc.getId();

        Optional<Schedule> result = scheduleRepository.findById(scheduleId);
        result.ifPresent(value -> assertSame(schedule, value));
    }

    @DisplayName("월간 일정 조회")
    @Test
    void findByScheduleByMonthly() {

    }

    @DisplayName("전부 찾아오는 테스트")
    @Test
    void findAll() {
        List<Schedule> all = scheduleRepository.findAll();
        assertNotNull(all);
    }

    @Test
    void findByAttendeeIdAndEndDatetimeBeforeAndStartDatetimeAfter() {

        Schedule schedule1 = Schedule.builder()
            .startDatetime(Instant.parse("2024-04-25T09:30:00Z"))
            .endDatetime(Instant.parse("2024-04-25T10:10:00Z"))
            .name("schedule1")
            .organizerId(1)
            .color(2)
            .isDeleted(false)
            .type(ScheduleType.PERSONAL)
            .isPublic(true)
            .build();

        Attendee attendee1 = Attendee.builder()
            .memberId(1)
            .isRequired(true)
            .status(AttendeeStatus.ACCEPTED)
            .schedule(schedule1)
            .build();
        Attendee attendee2 = Attendee.builder()
            .memberId(2)
            .isRequired(true)
            .status(AttendeeStatus.ACCEPTED)
            .schedule(schedule1)
            .build();
        Attendee attendee3 = Attendee.builder()
            .memberId(3)
            .isRequired(true)
            .status(AttendeeStatus.ACCEPTED)
            .schedule(schedule1)
            .build();

        schedule1.setAttendees(List.of(attendee1, attendee2, attendee3));

        Schedule save = scheduleRepository.save(schedule1);
        log.info("save: {}", save.getId());

        Instant start = Instant.parse("2024-04-25T10:00:00Z");
        Instant end = Instant.parse("2024-04-25T15:00:00Z");
        List<Schedule> schedules = scheduleRepository.findAcceptedSchedulesByAttendeeIdAndEndDatetimeBeforeAndStartDatetimeAfter(
            1, start, end);

        assertEquals(1, schedules.size());
        assertEquals(save.getId(), schedules.get(0).getId());
        assertEquals("schedule1", schedules.get(0).getName());
    }
}