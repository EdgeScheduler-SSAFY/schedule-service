package com.edgescheduler.scheduleservice.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
public class MeetingUpdateMessage extends KafkaEventMessage {

    private Long scheduleId;
    private String scheduleName;
    private Integer organizerId;
    private String organizerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Integer> attendeeIds;
    private List<Integer> maintainedAttendeeIds;
    private List<Integer> addedAttendeeIds;
    private List<Integer> removedAttendeeIds;
    private List<UpdatedField> updatedFields;

    public enum UpdatedField {
        TIME,
        TITLE,
        DESCRIPTION
    }
}
