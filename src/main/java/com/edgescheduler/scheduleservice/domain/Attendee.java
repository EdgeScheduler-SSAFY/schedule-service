package com.edgescheduler.scheduleservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Attendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Integer memberId;

    @NotNull
    private Boolean isRequired;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AttendeeStatus status;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @OneToOne
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    public void updateStatus(AttendeeStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public void updateProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}
