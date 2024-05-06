package com.edgescheduler.scheduleservice.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
public class MeetingCreateMessage extends KafkaEventMessage {

    private Long scheduleId;
    private String scheduleName;
    private Integer organizerId;
    private String organizerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Integer> attendeeIds;
}
