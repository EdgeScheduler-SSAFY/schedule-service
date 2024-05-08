package com.edgescheduler.scheduleservice.repository;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.edgescheduler.scheduleservice.domain.Attendee;
import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import com.edgescheduler.scheduleservice.domain.Schedule;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest
public class AttendeeRepositoryTest {

    @Autowired
    private AttendeeRepository AttendeeRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @DisplayName("일정으로 참석자 조회하기")
    @Test
    void findBySchedule() {
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

        List<Attendee> result = AttendeeRepository.findBySchedule(savedSchedule);

        assertIterableEquals(attendees, result);
    }
}
