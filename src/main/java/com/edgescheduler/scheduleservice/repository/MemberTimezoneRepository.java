package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.MemberTimezone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTimezoneRepository extends JpaRepository<MemberTimezone, Integer> {

}
