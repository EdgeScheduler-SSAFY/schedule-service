package com.edgescheduler.scheduleservice.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ErrorCode {

    EXAMPLE_ERROR(HttpStatus.BAD_REQUEST, "EX001", "This is an example error"),
    SCHEDULE_UPDATE_NO_QUALIFICATION_ERROR(HttpStatus.BAD_REQUEST, "SC001", "수정 권한이 없습니다."),
    TIMEZONE_NOT_FOUND(HttpStatus.BAD_REQUEST, "TZ001", "ZoneId를 찾을 수 없습니다.");
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
