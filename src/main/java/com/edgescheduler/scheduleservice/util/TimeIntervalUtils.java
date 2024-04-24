package com.edgescheduler.scheduleservice.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class TimeIntervalUtils {

    // 주어진 시간 범위를 15분 간격으로 나누어 반환
    public static long calculateIntervalCount(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end) / 15;
    }

    // 주어진 시간을 다음 15분 간격으로 조정
    public static LocalDateTime adjustToNextQuarterHour(LocalDateTime dateTime) {
        int minute = dateTime.getMinute();
        int additionalMinutes = (15 - (minute % 15)) % 15;
        return dateTime.plusMinutes(additionalMinutes);
    }

    // 주어진 시간을 이전 15분 간격으로 조정
    public static LocalDateTime adjustToPreviousQuarterHour(LocalDateTime dateTime) {
        int minute = dateTime.getMinute();
        int subtractMinutes = minute % 15;
        return dateTime.minusMinutes(subtractMinutes);
    }

}
