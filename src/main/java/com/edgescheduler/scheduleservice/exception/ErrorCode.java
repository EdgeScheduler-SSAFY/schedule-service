package com.edgescheduler.scheduleservice.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ErrorCode {

    EXAMPLE_ERROR(HttpStatus.BAD_REQUEST, "EX001", "This is an example error"),
    SCHEDULE_UPDATE_NO_QUALIFICATION_ERROR(HttpStatus.BAD_REQUEST, "SC001", "수정 권한이 없습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SC002", "일정을 찾을 수 없습니다."),
    ATTENDEE_NOT_FOUND(HttpStatus.NOT_FOUND, "AT001", "참석자를 찾을 수 없습니다."),
    TIMEZONE_NOT_FOUND(HttpStatus.BAD_REQUEST, "TZ001", "ZoneId를 찾을 수 없습니다."),
    INVALID_INTERVAL_COUNT(HttpStatus.BAD_REQUEST, "SC002", "기간이 회의 시간보다 짧습니다."),
    INVALID_PROPOSAL(HttpStatus.BAD_REQUEST, "SC003", "제안이 올바르지 않습니다.");
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public ApplicationException build() {
        return new ApplicationException(httpStatus, code, message);
    }

    public ApplicationException build(Object... args) {
        return new ApplicationException(httpStatus, code, message.formatted(args));
    }
}
