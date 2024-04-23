package com.edgescheduler.scheduleservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
            .startDatetime(Instant.parse("2024-04-24T11:00:00Z"))
            .endDatetime(Instant.parse("2024-04-24T11:00:00Z"))
            .googleCalendarId("googleCalendarId1")
            .isPublic(true)
            .build();

        scheduleRepository.save(schedule);

        assertEquals(scheduleRepository.findAll().size(), 1);
    }

    @DisplayName("일정 상세 조회")
    @Test
    void findByScheduleId() {
        Schedule schedule = Schedule.builder()
            .organizerId(1)
            .name("일정명")
            .description("일정 내용")
            .type(ScheduleType.valueOf("MEETING"))
            .startDatetime(Instant.parse("2024-04-24T11:00:00Z"))
            .endDatetime(Instant.parse("2024-04-24T11:00:00Z"))
            .googleCalendarId("googleCalendarId1")
            .isPublic(true)
            .build();
        Schedule sc = scheduleRepository.save(schedule);

        Long ScheduleId = sc.getId();

        Optional<Schedule> result = scheduleRepository.findById(ScheduleId);
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