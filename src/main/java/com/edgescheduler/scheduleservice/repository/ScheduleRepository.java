package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Schedule;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 일정 상세 조회
    public Optional<Schedule> findById(Long Id);
}
