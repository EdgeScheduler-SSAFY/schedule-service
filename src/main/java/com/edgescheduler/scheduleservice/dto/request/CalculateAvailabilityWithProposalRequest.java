package com.edgescheduler.scheduleservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class CalculateAvailabilityWithProposalRequest {

    @Setter
    Integer retrieverId;
    @NotNull
    Long scheduleId;
    @NotNull
    LocalDateTime startDatetime;
    @NotNull
    LocalDateTime endDatetime;
}
