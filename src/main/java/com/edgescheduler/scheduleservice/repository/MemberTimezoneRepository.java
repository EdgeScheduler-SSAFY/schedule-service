package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.MemberTimezone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberTimezoneRepository extends JpaRepository<MemberTimezone, Integer> {

}
