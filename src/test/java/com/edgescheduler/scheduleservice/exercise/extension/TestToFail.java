package com.edgescheduler.scheduleservice.exercise.extension;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  @TestToFail annotation
 *  - 테스트가 실패하면 통과 처리되도록 설정한 사용자 정의 어노테이션
 *  - 사용하려는 테스트 클래스에 @ExtendWith(TestToFailExtension.class) 어노테이션을 추가해야 함
 *  - @TestToFail annotation을 사용한 테스트 메서드의 마지막에 UnintendedSuccessException을 throw하여 테스트 실패 처리
 *
 *  사용 예시
 *  ```
 *  @ExtendWith(TestToFailExtension.class)
 *  public class JUnitAssertionsExerciseTest {
 *      @TestToFail
 *      void failTest() {
 *        assertEquals(1, 2);
 *        throw new UnintendedSuccessException();
 *      }
 *      ...
 * ```
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
public @interface TestToFail {
}
