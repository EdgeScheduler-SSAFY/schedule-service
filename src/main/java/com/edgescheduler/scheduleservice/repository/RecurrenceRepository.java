package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecurrenceRepository extends JpaRepository<Recurrence, Long> {

}
