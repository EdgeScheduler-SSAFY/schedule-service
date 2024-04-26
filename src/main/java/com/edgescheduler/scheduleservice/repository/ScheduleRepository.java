package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Schedule;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s JOIN s.attendees a "
        + "WHERE a.memberId = :attendeeId "
        + "AND a.status = 'ACCEPTED' "
        + "AND s.endDatetime > :start "
        + "AND s.startDatetime < :end ")
    List<Schedule> findAcceptedSchedulesByAttendeeIdAndEndDatetimeBeforeAndStartDatetimeAfter(
        Integer attendeeId,
        Instant start, Instant end);
}
