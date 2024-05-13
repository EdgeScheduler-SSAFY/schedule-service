package com.edgescheduler.scheduleservice.message;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class MeetingUpdateMessage extends KafkaEventMessage {

    private Long scheduleId;

    private String scheduleName;

    private Integer organizerId;

    private String organizerName;

    @Setter
    private LocalDateTime previousStartTime;    // 변경되지 않은 경우 null

    @Setter
    private LocalDateTime previousEndTime;      // 변경되지 않은 경우 null

    @Setter
    private LocalDateTime updatedStartTime;     // 변경되지 않은 경우 원래 시간

    @Setter
    private LocalDateTime updatedEndTime;       // 변경되지 않은 경우 원래 시간

    private Integer runningTime;

    private List<Integer> maintainedAttendeeIds;

    private List<Integer> addedAttendeeIds;

    private List<Integer> removedAttendeeIds;

    @Setter
    private List<UpdatedField> updatedFields;

    public enum UpdatedField {
        TIME,
        TITLE,
        DESCRIPTION
    }
}
