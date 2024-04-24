package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Attendee;
import com.edgescheduler.scheduleservice.domain.Schedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

    List<Attendee> findBySchedule(Schedule schedule);
}
