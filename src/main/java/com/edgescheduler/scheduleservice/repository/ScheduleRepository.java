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

    @Query("SELECT s FROM Schedule s WHERE s.parentSchedule = :schedule")
    List<Schedule> findScheduleByParentSchedule(Schedule schedule);

    @Query("SELECT s FROM Schedule s WHERE s.organizerId = :organizerId AND s.type != 'MEETING' AND s.isDeleted = false")
    List<Schedule> findSchedulesExceptMeetingByOrganizerId(Integer organizerId);

    // 수정 또는 삭제 된 회의 외 일정
    @Query("SELECT s FROM Schedule s WHERE s.parentSchedule IS NOT NULL AND s.organizerId = :organizerId AND s.type != 'MEETING'")
    List<Schedule> findModifiedOrDeletedNonMeetingSchedulesByOrganizerId(Integer organizerId);
}
