package com.edgescheduler.scheduleservice.message;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class MeetingUpdateMessage extends KafkaEventMessage {

    private Long scheduleId;
    private String scheduleName;
    private Integer organizerId;
    private String organizerName;
    private LocalDateTime previousStartTime;    // 변경되지 않은 경우 null
    private LocalDateTime previousEndTime;      // 변경되지 않은 경우 null
    private LocalDateTime updatedStartTime;
    private LocalDateTime updatedEndTime;
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
