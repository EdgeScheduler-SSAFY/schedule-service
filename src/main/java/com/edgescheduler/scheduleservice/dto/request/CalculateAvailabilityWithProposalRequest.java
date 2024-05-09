package com.edgescheduler.scheduleservice.dto.request;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CalculateAvailabilityWithProposalRequest {

    Integer retrieverId;
    Long scheduleId;
    LocalDateTime startDatetime;
    LocalDateTime endDatetime;
}
