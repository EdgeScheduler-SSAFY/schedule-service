package com.edgescheduler.scheduleservice.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Builder;
import org.springframework.stereotype.Component;

@Component
public class TimeIntervalUtils {

    /**
     * 주어진 시간이 주어진 시간 범위 내의 몇 번째 15분 간격에 속하는지 계산
     *
     * @param start  시작 범위
     * @param end    종료 범위
     * @param target 대상 시간
     * @return 15분 간격 인덱스
     */
    public static IntervalIndex calculateIntervalIndexWithinPeriod(LocalDateTime start,
        LocalDateTime end,
        LocalDateTime target) {

        LocalDateTime adjustedStart = adjustToNextQuarterHour(start);
        long intervalCount = calculateAdjustedIntervalCount(start, end);

        if (target.isBefore(start)) {
            return IntervalIndex.builder()
                .index(0)
                .onBoundary(true)
                .build();
        }
        if (target.isAfter(end)) {
            return IntervalIndex.builder()
                .index((int) intervalCount)
                .onBoundary(true)
                .build();
        }
        long minutes = getMinuteDuration(adjustedStart, target);
        return IntervalIndex.builder()
            .index((int) (minutes / 15))
            .onBoundary(minutes % 15 == 0)
            .build();
    }

    // 주어진 시간 범위를 15분 간격으로 나누어 반환
    public static long calculateAdjustedIntervalCount(LocalDateTime start, LocalDateTime end) {
        LocalDateTime adjustedStart = adjustToNextQuarterHour(start);
        LocalDateTime adjustedEnd = adjustToPreviousQuarterHour(end);
        return getMinuteDuration(adjustedStart, adjustedEnd) / 15;
    }

    public static long calculateIntervalCount(LocalDateTime start, LocalDateTime end) {
        return getMinuteDuration(start, end) / 15;
    }

    public static long getMinuteDuration(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    // 주어진 시간이 15분 단위(0,15,30,45)가 아니라면 이후 가장 가까운 15분 단위 시간으로 조정
    public static LocalDateTime adjustToNextQuarterHour(LocalDateTime dateTime) {
        int minute = dateTime.getMinute();
        int additionalMinutes = (15 - (minute % 15)) % 15;
        return dateTime.plusMinutes(additionalMinutes);
    }

    // 주어진 시간이 15분 단위(0,15,30,45)가 아니라면 이전 가장 가까운 15분 단위 시간으로 조정
    public static LocalDateTime adjustToPreviousQuarterHour(LocalDateTime dateTime) {
        int minute = dateTime.getMinute();
        int subtractMinutes = minute % 15;
        return dateTime.minusMinutes(subtractMinutes);
    }

    @Builder
    public static record IntervalIndex(int index, boolean onBoundary) {

    }
}
