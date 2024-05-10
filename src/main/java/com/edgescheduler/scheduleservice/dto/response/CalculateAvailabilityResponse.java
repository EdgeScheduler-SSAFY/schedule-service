package com.edgescheduler.scheduleservice.dto.response;

import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.vo.IntervalStatus;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class CalculateAvailabilityResponse {

    private List<IndividualSchedulesAndAvailability> individualSchedulesAndAvailabilities;
    private List<MeetingRecommendation> fastestMeetings;
    private List<MeetingRecommendation> mostParticipantsMeetings;
    private List<MeetingRecommendation> mostParticipantsInWorkingHourMeetings;

    @Getter
    @Builder
    public static class IndividualSchedulesAndAvailability {

        private Integer memberId;
        private Boolean isRequired;
        private List<ScheduleEntry> schedules;
        @Setter
        private IntervalStatus[] availability;
    }

    @Getter
    @Builder
    @EqualsAndHashCode
    public static class ScheduleEntry {

        private String name;
        private Integer startIndexInclusive;
        private Integer endIndexExclusive;
        private ScheduleType type;
        private Boolean isPublic;
    }
}
