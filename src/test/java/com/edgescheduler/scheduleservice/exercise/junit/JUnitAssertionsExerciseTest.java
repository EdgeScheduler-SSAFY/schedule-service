package com.edgescheduler.scheduleservice.exercise.junit;

import com.edgescheduler.scheduleservice.exercise.extension.TestToFail;
import com.edgescheduler.scheduleservice.exercise.extension.TestToFailExtension;
import com.edgescheduler.scheduleservice.exercise.extension.UnintendedSuccessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
/**
 * @TestToFail annotation : 테스트가 실패하면 통과 처리되도록 설정한 사용자 정의 어노테이션
 */
@ExtendWith(TestToFailExtension.class)
public class JUnitAssertionsExerciseTest {

    /**
     * fail() : 테스트 실패 처리
     * fail(String message) : 테스트 실패 처리, 실패 이유 메시지 전달
     * fail(Throwable cause) : 테스트 실패 처리, 실패 이유 예외 전달
     */
    @TestToFail
    void failTest() {
        var condition = true;
        if (condition) {
            fail();
        }

        throw new UnintendedSuccessException();
    }

    @TestToFail
    void failMessageTest() {
        var condition = true;
        if (condition) {
            fail("실패한 이유를 메시지로 전달");
        }

        throw new UnintendedSuccessException();
    }

    @TestToFail
    void failExceptionTest() {
        var condition = true;
        if (condition) {
            var exception = new RuntimeException("실패한 이유를 예외로 전달");
            fail(exception);
        }

        throw new UnintendedSuccessException();
    }


    /**
     * Assertions comparison methods
     *  - assertXXX() : 테스트 결과 검증
     *  - assertTrue / assertFalse : 참/거짓 검증
     *  - assertEquals / assertNotEquals : 동등성 검증, 객체의 경우 equals() 메서드로 비교
     *  - assertSame / assertNotSame : 동일성 검증, 객체의 참조가 같은지 확인
     *  - assertNull / assertNotNull : null 여부 검증
     *  - assertInstanceOf / assertNotInstanceOf : 타입 검증
     *  - assertArrayEquals : 배열 동등성 검증, 객체의 경우 equals() 메서드가 정의되어 있어야 함
     *  - assertIterableEquals : Iterable 동등성 검증, 객체의 경우 equals() 메서드가 정의되어 있어야 함
     *  - assertLinesMatch : 문자열 리스트 동등성 검증
     *
     *  비교에 사용될 parameter로는 검증할 값/객체 자체를 넣거나 Supplier를 이용해 lazy evaluation을 할 수 있음
     *  마지막 parameter로는 실패 시 출력할 메시지를 전달할 수 있음, 이 또한 Supplier<String> 이용 가능
     */

    /**
     * assertLinesMatch
     * - 문자열 List 또는 Stream을 비교하여 각 요소가 순서대로 일치하는지 검증
     * - 1차적으로 equals() 메서드로 비교하고, 일치하지 않는 경우 정규식으로 비교
     * - 정규식을 활용하여 특정 패턴을 검증할 때 유용
     */
    @Test
    void lineMatchTest() {
        var expected = List.of("abc", "[a-z]+", "[0-9]+");
        var actual = List.of("abc", "qwerasdfzxcv", "987149807143");
        assertLinesMatch(expected, actual);
    }


    /**
     * assertAll
     *  - 인자로 받은 여러 Executable을 모두 실행하고 검증을 수행
     *  - Exception을 throw하는 Executable이 포함된 경우, 테스트 실패 처리
     *  - 가변 인자, Collection, Stream 형태로 여러 Executable을 전달할 수 있음
     */
    @TestToFail
    void assertAllTest() {
        assertAll(
            () -> assertTrue(true),
            () -> assertEquals(1, 2),
            () -> assertArrayEquals(new int[]{1, 2, 3}, new int[]{1, 2, 3}),
            () -> assertLinesMatch(List.of("abc", "[a-z]+", "[0-9]+"), List.of("abc", "qwerasdfzxcv", "987149807143"))
        );

        throw new UnintendedSuccessException();
    }

    /**
     * assertThrows / assertDoesNotThrow
     *  - 특정 예외가 발생하는지 / 발생하지 않는지 검증
     *  - 인자로 주어진 Exception을 상속하는 Exception이 발생해도 통과
     *  - Executable이 아닌 ThrowingSupplier을 인자로 사용할 경우 반환값을 전달받아 이후 테스트 코드에 사용할 수 있음
     */
    @Test
    void assertThrowsTest() {
        assertThrows(RuntimeException.class, () -> {
            throw new IllegalArgumentException("예외 발생");
        });
    }

    @Test
    void assertDoesNotThrowTest() {
        String result = assertDoesNotThrow(() -> {
            return "예외 발생하지 않음";
        });

        assertEquals("예외 발생하지 않음", result);
    }

    /**
     * assertThrowsExactly : 인자로 전달받은 Throwable 클래스와 정확히 일치하는 예외가 발생하는지 검증 (상속 관계는 통과하지 않음)
     */
    @TestToFail
    void assertThrowsExactlyTest() {
        assertThrowsExactly(RuntimeException.class, () -> {
            throw new IllegalArgumentException("예외 발생");
        });

        throw new UnintendedSuccessException();
    }

    /**
     * assertTimeout
     * - 특정 시간 내에 실행이 완료되는지 검증
     * - Duration 객체를 이용해 시간을 지정하고, 실행 시간이 지정한 시간을 초과하면 테스트 실패 처리
     * - 실행 시간이 지정한 시간을 초과해도 테스트는 계속 진행되며, 테스트 결과는 실패 처리
     * - 두번째 인자로 Executable이 아닌 ThrowingSupplier를 사용할 경우 반환값을 전달받아 이후 테스트 코드에 사용할 수 있음
     */
    @Test
    void assertTimeoutTest() {
        String result = assertTimeout(Duration.ofMillis(1000), () -> {
            Thread.sleep(500);
            return "완료";
        });

        assertEquals("완료", result);
    }

    @TestToFail
    void assertTimeoutFailTest() {
        assertTimeout(Duration.ofMillis(1000), () -> {
            Thread.sleep(1500);
            log.info("실행 완료");
        });

        throw new UnintendedSuccessException();
    }

    /**
     * assertTimeoutPreemptively
     *  - 테스트 실행 중에 기대한 시간을 초과하면 즉시 테스트 실패 처리 (실행 중인 코드를 중단)
     *  - 테스트 실행 시간을 단축할 수 있음
     */
    @TestToFail
    void assertTimeoutPreemptivelyTest() {
        assertTimeoutPreemptively(Duration.ofMillis(1000), () -> {
            Thread.sleep(1500);
            log.info("실행 완료");
        });

        throw new UnintendedSuccessException();
    }
}
