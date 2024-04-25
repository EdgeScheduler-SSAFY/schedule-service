package com.edgescheduler.scheduleservice.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TimeIntervalUtilsTest {

    @Nested
    @DisplayName("같거나 이후인 15분 단위 시간으로 조정")
    class AdjustToNextQuarterTest {

        @DisplayName("이미 15분 단위 시간이면 그대로")
        @Test
        void noNeedToAdjustTest() {
            var dateTime = LocalDateTime.of(2024, 4, 25, 10, 0);
            assertEquals(
                LocalDateTime.of(2024, 4, 25, 10, 0),
                TimeIntervalUtils.adjustToNextQuarterHour(dateTime)
            );
        }

        @DisplayName("이후 시간 중 가장 가까운 15분 단위 시간으로 조정")
        @Test
        void adjustTest() {
            var dateTime = LocalDateTime.of(2024, 4, 25, 23, 59);
            assertEquals(
                LocalDateTime.of(2024, 4, 26, 0, 0),
                TimeIntervalUtils.adjustToNextQuarterHour(dateTime)
            );
        }
    }

    @Nested
    @DisplayName("같거나 이전인 15분 단위 시간으로 조정")
    class AdjustToPreviousQuarterTest {

        @DisplayName("이미 15분 단위 시간이면 그대로")
        @Test
        void noNeedToAdjustTest() {
            var dateTime = LocalDateTime.of(2024, 4, 25, 10, 0);
            assertEquals(
                LocalDateTime.of(2024, 4, 25, 10, 0),
                TimeIntervalUtils.adjustToPreviousQuarterHour(dateTime)
            );
        }

        @DisplayName("이전 시간 중 가장 가까운 15분 단위 시간으로 조정")
        @Test
        void adjustTest() {
            var dateTime = LocalDateTime.of(2024, 4, 25, 10, 5);
            assertEquals(
                LocalDateTime.of(2024, 4, 25, 10, 0),
                TimeIntervalUtils.adjustToPreviousQuarterHour(dateTime)
            );
        }
    }

    @DisplayName("15분 단위로 나눈 구간 개수 계산")
    @Test
    void calculateIntervalCount() {
        var start = LocalDateTime.of(2024, 4, 25, 9, 57);
        var end = LocalDateTime.of(2024, 4, 25, 13, 11);
        assertEquals(12, TimeIntervalUtils.calculateIntervalCount(start, end));
    }
}