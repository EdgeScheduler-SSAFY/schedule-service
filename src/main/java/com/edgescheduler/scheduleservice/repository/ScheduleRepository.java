package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {}
