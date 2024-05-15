package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Attendee;
import com.edgescheduler.scheduleservice.domain.Proposal;
import com.edgescheduler.scheduleservice.domain.Schedule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

    List<Attendee> findBySchedule(Schedule schedule);

    List<Attendee> findByMemberId(Integer memberId);

    Optional<Attendee> findByScheduleIdAndMemberId(Long scheduleId, Integer memberId);

    @Modifying
    @Query("UPDATE Attendee a SET a.proposal = NULL WHERE a.schedule = :schedule")
    void deleteProposalBySchedule(Schedule schedule);

    @Modifying
    @Query("UPDATE Attendee a SET a.proposal = NULL WHERE a.proposal = :proposal")
    void deleteOneProposalByProposal(Proposal proposal);

}
