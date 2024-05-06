package com.edgescheduler.scheduleservice.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class AttendeeResponseMessage extends KafkaEventMessage {

    private Long scheduleId;
    private String scheduleName;
    private Integer organizerId;
    private Integer attendeeId;
    private String attendeeName;
    private Response response;
}
