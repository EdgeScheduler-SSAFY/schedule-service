package com.edgescheduler.scheduleservice.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
public class AttendeeProposalMessage extends KafkaEventMessage {

    private Long scheduleId;
    private String scheduleName;
    private Integer organizerId;
    private Integer attendeeId;
    private String attendeeName;
    private LocalDateTime proposedStartTime;
    private LocalDateTime proposedEndTime;
    private String reason;
}
