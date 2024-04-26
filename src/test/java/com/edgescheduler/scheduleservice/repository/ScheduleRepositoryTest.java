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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

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
            () -> assertEquals(1, scheduleRepository.findAll().size())
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
}