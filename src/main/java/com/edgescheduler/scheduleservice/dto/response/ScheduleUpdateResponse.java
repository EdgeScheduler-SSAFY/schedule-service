package com.edgescheduler.scheduleservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleUpdateResponse {

    private Long scheduleId;
}
