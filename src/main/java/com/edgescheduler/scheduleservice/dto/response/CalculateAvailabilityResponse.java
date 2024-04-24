package com.edgescheduler.scheduleservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CalculateAvailabilityResponse {

    private List<individualScheduleEntry> schedules;
    private List<TokenizedTimeAvailability> tokenizedTimeAvailabilities;

    @Getter
    @Builder
    public static class individualScheduleEntry {

        private Integer memberId;
        private List<ScheduleEntry> schedules;
    }

    @Getter
    @Builder
    public static class ScheduleEntry {
        private String name;
        private LocalDateTime startDatetime;
        private LocalDateTime endDatetime;
        private Boolean isPublic;
    }

    @Getter
    @Builder
    public static class TokenizedTimeAvailability {
        private Integer availableMemberCount;
        private Integer availableRequiredMemberCount;
        private Integer availableMemberInWorkingHourCount;
        private Integer availableRequiredMemberInWorkingHourCount;
    }
}
