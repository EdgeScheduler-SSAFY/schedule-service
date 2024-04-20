package com.edgescheduler.scheduleservice.exercise.extension;

/**
 * UnintendedSuccessException
 *  - 실패되어야 하는 테스트가 성공했을 때 발생하는 예외
 *  - @TestToFail annotation을 사용한 테스트 메서드의 마지막에 throw
 */
public class UnintendedSuccessException extends RuntimeException {
    public UnintendedSuccessException() {
        super("실패되어야 하는 테스트가 성공했습니다.");
    }
}
