package com.edgescheduler.scheduleservice.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseScheduleProposal {
    private Boolean isAccepted;
}
