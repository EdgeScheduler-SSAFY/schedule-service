package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Schedule;
import com.edgescheduler.scheduleservice.vo.ScheduleVO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SimpleScheduleRepository extends JpaRepository<Schedule, Long>{

    List<ScheduleVO> findByMemberIdAndEndDatetimeBeforeAndStartDatetimeAfter(Integer memberId, Instant start, Instant end);
}
