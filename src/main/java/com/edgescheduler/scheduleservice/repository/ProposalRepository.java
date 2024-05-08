package com.edgescheduler.scheduleservice.repository;

import com.edgescheduler.scheduleservice.domain.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

}
