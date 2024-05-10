package com.edgescheduler.scheduleservice.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeTimeZoneMessage {

    private Integer memberId;
    private String zoneId;
}
