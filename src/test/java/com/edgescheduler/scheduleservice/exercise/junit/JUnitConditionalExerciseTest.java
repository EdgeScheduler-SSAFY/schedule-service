package com.edgescheduler.scheduleservice.exercise.junit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
//@Disabled
public class JUnitConditionalExerciseTest {

    /**
     *  @Disabled annotation
     *  - 테스트 무시 처리
     *  - 테스트 코드를 주석 처리하지 않고 테스트를 무시하고 싶을 때 사용
     *  - 메서드 레벨 / 클래스 레벨에 모두 사용 가능 (Nested 클래스에도 가능)
     */
    @Disabled
    @Test
    void disabledTest() {
        assertTrue(true);
    }

    @Disabled("#3 이슈가 해결되면 테스트 진행")
    @Test
    void disabledWithReasonTest() {
        assertTrue(true);
    }

    @Disabled("XXXService 클래스 구현 전까지 테스트 진행 중단")
    @Nested
    class DisabledNestedTest {
        @Test
        void disabledNestedTest() {
            assertTrue(true);
        }
    }


    /**
     *  @Disabled* / @Enabled* annotations
     *  - 특정 조건에 따라 테스트를 무시하거나 진행하는 어노테이션 제공
     *   - OS 종류, Java 버전, 환경 변수, 시스템 프로퍼티 등
     */
    @DisabledOnOs(OS.WINDOWS)
    @Test
    void disabledOnOsTest() {
        assertTrue(true);
    }

    @EnabledOnOs(value = OS.MAC, architectures = "aarch64", disabledReason = "Mac OS / arm 아키텍처에서만 테스트 진행")
    @Test
    void enabledOnOsTest() {
        assertTrue(true);
    }

    @DisabledOnJre({JRE.JAVA_8, JRE.JAVA_17})
    @Test
    void disabledOnJreTest() {
        assertTrue(true);
    }

    @EnabledForJreRange(min = JRE.JAVA_11, max = JRE.JAVA_16, disabledReason = "Java 11 ~ 16 버전에서만 테스트 진행")
    @Test
    void enabledForJreRangeTest() {
        assertTrue(true);
    }

    @DisabledIfSystemProperty(named = "os.arch", matches = "[a-z1-9]+")
    @Test
    void disabledIfSystemPropertyTest() {
        assertTrue(true);
    }

    @EnabledIfSystemProperties({
            @EnabledIfSystemProperty(named = "os.arch", matches = ".*64.*"),
            @EnabledIfSystemProperty(named = "os.name", matches = "Mac.*")
    })
    @Test
    void enabledIfSystemPropertiesTest() {
        assertTrue(true);
    }

    /**
     *  @EnabledIfEnvironmentVariable / @DisabledIfEnvironmentVariable
     *  - 환경 변수에 따라 테스트를 진행하거나 무시
     *  - IntelliJ의 Run/Debug Configuration에서 환경 변수를 설정할 수 있음
     */
    @EnabledIfEnvironmentVariable(named = "TEST_ENV", matches = "test")
    @Test
    void enabledIfEnvironmentVariableTest() {
        assertTrue(true);
    }

    /**
     *  @EnabledIf / @DisabledIf
     *  - 커스텀 조건에 따라 테스트를 진행하거나 무시
     */
    boolean condition() {
        return false;
    }

    @EnabledIf(value = "condition", disabledReason = "커스텀 조건에 따라 테스트 진행")
    @Test
    void enabledIfTest() {
        assertTrue(true);
    }
}
