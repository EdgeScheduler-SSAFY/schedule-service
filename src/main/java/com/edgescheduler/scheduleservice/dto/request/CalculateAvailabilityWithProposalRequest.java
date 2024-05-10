package com.edgescheduler.scheduleservice.dto.request;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class CalculateAvailabilityWithProposalRequest {

    @Setter
    Integer retrieverId;
    Long scheduleId;
    LocalDateTime startDatetime;
    LocalDateTime endDatetime;
}
