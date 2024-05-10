package com.edgescheduler.scheduleservice.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class CalculateAvailabilityRequest {

    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;

    @Setter
    private Integer organizerId;
    private Integer runningTime;            // in minutes
    private List<CalculatingMember> memberList;

    @Getter
    @Builder
    public static class CalculatingMember {

        private Integer memberId;
        private Boolean isRequired;
    }
}
