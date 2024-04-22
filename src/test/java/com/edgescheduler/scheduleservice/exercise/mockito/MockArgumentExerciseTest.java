package com.edgescheduler.scheduleservice.exercise.mockito;

import com.edgescheduler.scheduleservice.service.DummyService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 *  ArgumentMatchers 클래스를 활용해 Stubbing을 할 때 인자를 유연하게 지정할 수 있다.
 */
public class MockArgumentExerciseTest {

    /**
     *  eq(T value) : 지정된 값과 동일한 값을 가지는 인자를 지정
     *  - eq 메서드를 사용해 인자를 전달하면, 해당 인자가 지정된 값과 동일한지 비교
     */
    @Test
    void mockArgumentEqTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam(eq("test")))
                .thenReturn("mock test");

        assertEquals("mock test", dummyService.dummyMethodWithParam("test"));
    }

    /**
     *  any(Class<T> type) / isA(Class<T> type) : 지정된 타입의 인자를 지정, 두 메서드는 동일하게 동작.
     *  - 인자를 전달하면, 해당 인자가 지정된 타입과 동일한지 비교
     *  - null은 허용하지 않음
     *
     *  nullable(Class<T> clazz) : 지정된 타입의 인자를 지정, null을 허용
     */
    @Test
    void mockArgumentAnyTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam(any(String.class)))
                .thenReturn("mock any");

        assertEquals("mock any", dummyService.dummyMethodWithParam("test"));
        assertEquals("mock any", dummyService.dummyMethodWithParam("test2"));

        when(dummyService.dummyMethodWithParam(isA(String.class)))
                .thenReturn("mock isA");

        assertEquals("mock isA", dummyService.dummyMethodWithParam("test3"));

        when(dummyService.dummyMethodWithParam(nullable(String.class)))
                .thenReturn("mock nullable");

        assertEquals("mock nullable", dummyService.dummyMethodWithParam(null));
    }

    /**
     *  any*() : 해당하는 타입의 인자를 지정, primitive 타입과 주요 클래스 타입에 대해 지원
     *  - primitive 타입: boolean, byte, char, double, float, int, long, short
     *  - 주요 클래스 타입: String, Collection, List, Set, Map, Iterable
     */
    @Test
    void mockArgumentAnyPrimitiveTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam(anyString()))
                .thenReturn("mock anyString");

        assertEquals("mock anyString", dummyService.dummyMethodWithParam("test"));
    }

    /**
     *  String 타입에 대한 helper 메서드
     *  - contains(String substring) : 지정된 문자열을 포함하는 인자를 지정
     *  - startsWith(String prefix) : 지정된 문자열로 시작하는 인자를 지정
     *  - endsWith(String suffix) : 지정된 문자열로 끝나는 인자를 지정
     *  - matches(String regex) : 지정된 정규식과 일치하는 인자를 지정, Pattern 객체도 전달 가능
     */
    @Test
    void mockArgumentContainsTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam(contains("test")))
                .thenReturn("mock contains");

        assertEquals("mock contains", dummyService.dummyMethodWithParam("test"));
        assertEquals("mock contains", dummyService.dummyMethodWithParam("test2"));
    }

    @Test
    void mockArgumentStartsWithTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam(startsWith("test")))
                .thenReturn("mock startsWith");

        assertEquals("mock startsWith", dummyService.dummyMethodWithParam("test"));
        assertEquals("mock startsWith", dummyService.dummyMethodWithParam("test2"));
    }

    @Test
    void mockArgumentEndsWithTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam(endsWith("test")))
                .thenReturn("mock endsWith");

        assertEquals("mock endsWith", dummyService.dummyMethodWithParam("first test"));
        assertEquals("mock endsWith", dummyService.dummyMethodWithParam("second test"));
    }

    @Test
    void mockArgumentMatchesTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam(matches("[0-9]+ test")))
                .thenReturn("mock matches");

        assertEquals("mock matches", dummyService.dummyMethodWithParam("1 test"));
        assertEquals("mock matches", dummyService.dummyMethodWithParam("2 test"));
    }

    /**
     *  argThat(ArgumentMatcher<T> matcher) : 지정된 ArgumentMatcher를 사용해 인자를 지정
     *  - ArgumentMatcher는 함수형 인터페이스로, `boolean matches(T argument)` 메서드를 구현해 인자를 비교
     *  - matches 메서드가 false를 반환하면 Stubbing이 적용되지 않음 (기본값을 반환)
     */
    @Test
    void mockArgumentArgThatTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam(argThat(argument -> argument.length() < 5)))
                .thenReturn("mock argThat");

        when(dummyService.dummyMethodWithParamInt(argThat(argument -> argument < 5)))
                .thenReturn(1);

        assertEquals("mock argThat", dummyService.dummyMethodWithParam("str"));
        assertNull(dummyService.dummyMethodWithParam("string"));
        assertEquals(1, dummyService.dummyMethodWithParamInt(4));
    }
}
