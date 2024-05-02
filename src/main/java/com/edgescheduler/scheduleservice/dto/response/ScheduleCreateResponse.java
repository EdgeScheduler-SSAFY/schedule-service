package com.edgescheduler.scheduleservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ScheduleCreateResponse {

    private Long scheduleId;
}
