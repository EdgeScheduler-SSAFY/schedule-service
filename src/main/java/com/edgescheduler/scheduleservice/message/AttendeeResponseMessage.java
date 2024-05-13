package com.edgescheduler.scheduleservice.message;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class AttendeeResponseMessage extends KafkaEventMessage {

    private Long scheduleId;
    private String scheduleName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer organizerId;
    private Integer attendeeId;
    private String attendeeName;

    @Setter
    private Response response;
}
