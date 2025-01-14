package com.edgescheduler.scheduleservice.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ErrorCode {

    EXAMPLE_ERROR(HttpStatus.BAD_REQUEST, "EX001", "This is an example error"),
    SCHEDULE_UPDATE_NO_QUALIFICATION_ERROR(HttpStatus.BAD_REQUEST, "SC001", "수정 권한이 없습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SC002", "일정을 찾을 수 없습니다."),
    SCHEDULE_NOT_REGISTERED_FOR_RECURRENCE_DAY_IS_EMPTY(HttpStatus.BAD_REQUEST, "SC003", "주 반복 일정 등록 시 반복 요일이 비어있습니다."),
    ATTENDEE_NOT_FOUND(HttpStatus.NOT_FOUND, "AT001", "참석자를 찾을 수 없습니다."),
    ATTENDEE_DUPLICATED_DECISION(HttpStatus.BAD_REQUEST, "AT002", "이미 동일한 응답을 하였습니다."),
    PROPOSAL_DIFFERENT_RUNNING_TIME(HttpStatus.BAD_REQUEST, "PR001", "회의 총 진행 시간을 변경할 수 없습니다."),
    TIMEZONE_NOT_FOUND(HttpStatus.BAD_REQUEST, "TZ001", "ZoneId를 찾을 수 없습니다."),
    INVALID_INTERVAL_COUNT(HttpStatus.BAD_REQUEST, "SC004", "기간이 회의 시간보다 짧습니다."),
    INVALID_PROPOSAL(HttpStatus.BAD_REQUEST, "SC005", "제안이 올바르지 않습니다."),
    SCHEDULE_NOT_REGISTERED_FOR_START_DATETIME_IS_AFTER_END_DATETIME(HttpStatus.BAD_REQUEST,"SC006", "시작 시간이 종료 시간보다 늦습니다.");
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
