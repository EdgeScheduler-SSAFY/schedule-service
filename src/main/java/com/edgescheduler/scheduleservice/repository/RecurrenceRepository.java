package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceRepository extends JpaRepository<Recurrence, Long> {

}
