package com.edgescheduler.scheduleservice.dto.request;


import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleDeleteRequest {
    private ScheduleDeleteRange deleteRange;
    private LocalDateTime deleteDatetime;

    public enum ScheduleDeleteRange{
        ALL, ONE, AFTERALL
    }
}
