package com.edgescheduler.scheduleservice.exercise.junit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.*;

public class JUnitAssumptionsExerciseTest {
    private static final Logger log = LoggerFactory.getLogger(JUnitAssumptionsExerciseTest.class);

    /**
     * Assumptions class
     *  - 테스트 코드를 계속 진행할지를 결정하는 메서드를 제공하는 클래스
     *  - 계속 진행되지 않는다면 해당 테스트는 무시(ignored) 처리
     *  - 특정 환경 변수가 설정되어 있는지, 특정 파일이 존재하는지 등 테스트를 진행하기 위한 전제 조건을 설정
     */

    /**
     * abort() : 테스트 중단 처리
     */
    @Test
    void abortTest() {
        var condition = true;
        if (condition) {
            abort("테스트 중단 처리");
        }
    }

    /**
     * assumeTrue / assumeFalse : 특정 조건이 참/거짓인 경우에만 테스트 진행
     */
    @Test
    void assumeTrueTest() {
        var assumption = true;
        assumeTrue(assumption, "테스트 중단 처리");     // 테스트 진행
        assumeFalse(assumption, "테스트 중단 처리");    // 테스트 중단
    }

    /**
     * assumingThat : assumption이 참인 경우에만 테스트 진행
     */
    @Test
    void assumeThatTest() {
        var javaVersion = System.getProperties()
                    .get("java.specification.version");

        assumingThat("17".equals(javaVersion), () -> {
            log.info("Java 17 환경에서만 실행되는 테스트");
        });
    }
}
