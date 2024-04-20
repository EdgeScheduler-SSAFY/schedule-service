package com.edgescheduler.scheduleservice.exercise.junit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Slf4j
/**
 * @DisplayName annotation
 *  - 테스트의 이름을 지정하는 어노테이션
 *  - 클래스 / 메서드 레벨에 사용 가능
 */
@DisplayName("테스트 클래스 이름 설정")
public class JUnitUtilityExerciseTest {

    @DisplayName("테스트 메서드 이름 설정")
    @Test
    void testMethod() {
        log.info("테스트 메서드 실행");
    }

    /**
     * @Tag annotation
     *  - 테스트에 태그를 지정하는 어노테이션
     *  - 클래스 / 메서드 레벨에 사용 가능
     *  - 태그를 지정하여 테스트 그룹을 만들어 테스트 실행
     *  - JUnit Test 생성 시 Tags 필터를 통해 특정 태그를 가진 테스트만 실행 가능
     */

    @Tag("tag1")
    @Test
    void testMethodWithTag1() {
        log.info("tag1 태그를 가진 테스트 메서드 실행");
    }

    @Tag("tag2")
    @Test
    void testMethodWithTag2() {
        log.info("tag2 태그를 가진 테스트 메서드 실행");
    }
}
