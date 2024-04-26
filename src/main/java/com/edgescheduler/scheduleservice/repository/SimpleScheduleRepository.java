package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.vo.ScheduleVO;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface SimpleScheduleRepository {

    @Query("SELECT s FROM Schedule s JOIN s.attendees a "
        + "WHERE a.id = :memberId "
        + "AND s.endDatetime > :start "
        + "AND s.startDatetime < :end")
    List<ScheduleVO> findByMemberIdAndEndDatetimeBeforeAndStartDatetimeAfter(Integer memberId,
        Instant start, Instant end);
}
