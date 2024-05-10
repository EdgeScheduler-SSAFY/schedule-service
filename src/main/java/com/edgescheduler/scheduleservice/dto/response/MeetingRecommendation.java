package com.edgescheduler.scheduleservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class MeetingRecommendation {

    private RecommendType recommendType;
    private LocalDateTime start;
    private LocalDateTime end;
    private Integer startIndex;
    private Integer endIndex;
    private List<Integer> availableMemberIds;
    private List<Integer> availableMemberInWorkingHourIds;

    public enum RecommendType {
        FASTEST, MOST_PARTICIPANTS, MOST_PARTICIPANTS_IN_WORKING_HOUR
    }

}
