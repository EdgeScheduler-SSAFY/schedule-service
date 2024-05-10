package com.edgescheduler.scheduleservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class MeetingRecommendation {

    private RecommendType recommendType;
    private Integer startIndexInclusive;
    private Integer endIndexExclusive;

    public enum RecommendType {
        FASTEST, MOST_PARTICIPANTS, MOST_PARTICIPANTS_IN_WORKING_HOUR
    }

}
