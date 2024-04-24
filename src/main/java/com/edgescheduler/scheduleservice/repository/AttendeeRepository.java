package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Attendee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

}
