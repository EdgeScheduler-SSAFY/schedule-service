package com.edgescheduler.scheduleservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class CalculateAvailabilityRequest {

    @NotNull
    private LocalDateTime startDatetime;
    @NotNull
    private LocalDateTime endDatetime;

    @Setter
    private Integer organizerId;
    @NotNull
    private Integer runningTime;            // in minutes
    @NotNull
    private List<CalculatingMember> memberList;

    @Getter
    @Builder
    public static class CalculatingMember {

        private Integer memberId;
        private Boolean isRequired;
    }
}
