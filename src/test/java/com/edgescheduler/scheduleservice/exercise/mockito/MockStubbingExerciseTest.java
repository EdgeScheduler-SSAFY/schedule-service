package com.edgescheduler.scheduleservice.exercise.mockito;

import com.edgescheduler.scheduleservice.service.DummyService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Slf4j
/**
 *  MockStubbingExerciseTest
 *  - Mocking 및 Stubbing 연습 테스트
 *  - Mocking : 테스트 대상 객체를 직접 사용하지 않고 가짜 객체를 대신 사용하는 것
 *  - Stubbing : Mock 객체의 행동을 사전에 지정하는 것
 */
public class MockStubbingExerciseTest {

    /**
     *  mock() : Mock 객체 생성
     *
     */

    /**
     *  when-then* 패턴
     *  - when : Mock 객체의 메소드 호출을 지정, OnGoingStubbing 객체 반환
     *  - then* : 지정된 메소드 호출에 대한 다음 동작을 지정, OnGoingStubbing 객체 반환하여 체이닝 가능
     *  ex) Mockito.when(mock.method())
     *      .thenReturn(value)
     *      .thenThrow(exception);
     */

    /**
     *  thenReturn : 지정된 메소드 호출에 대한 반환값을 지정
     *  - 하나 이상의 값을 지정할 수 있으며, 메서드 호출시 순차적으로 반환
     *  - 마지막 값 이후 호출 시 마지막 값을 반복적으로 반환
     *  - thenReturn 메서드를 체이닝해도 동일하게 동작
     */

    @Test
    void mockStubbingTest() {

        DummyService dummyService = mock();

        when(dummyService.getDummy())
                .thenReturn("mock dummy")
                .thenReturn("mock dummy2")
                .thenReturn("mock dummy3", "mock dummy4");

        assertEquals("mock dummy", dummyService.getDummy());
        assertEquals("mock dummy2", dummyService.getDummy());
        assertEquals("mock dummy3", dummyService.getDummy());
        assertEquals("mock dummy4", dummyService.getDummy());
        assertEquals("mock dummy4", dummyService.getDummy());
    }

    /**
     *  thenThrow : 지정된 메소드 호출에 대한 예외를 지정
     *  - Throwable 객체나 예외 클래스를 인자로 전달할 수 있으며, 메서드 호출시 예외 발생
     *  - 하나 이상의 예외를 지정할 수 있으며, 메서드 호출시 순차적으로 예외 발생
     *  - 마지막 예외 이후 호출 시 마지막 예외 발생
     *  - thenThrow 메서드를 체이닝해도 동일하게 동작
     */
    @Test
    void mockStubbingThrowTest() {

        DummyService dummyService = mock();

        when(dummyService.getDummy())
                .thenThrow(new RuntimeException("mock exception"))
                .thenThrow(IllegalArgumentException.class)
                .thenThrow(new NumberFormatException("mock exception3"), new NullPointerException("mock exception4"));

        assertThrows(RuntimeException.class, dummyService::getDummy);
        assertThrows(IllegalArgumentException.class, dummyService::getDummy);
        assertThrows(NumberFormatException.class, dummyService::getDummy);
        assertThrows(NullPointerException.class, dummyService::getDummy);
        assertThrows(NullPointerException.class, dummyService::getDummy);
    }

    /**
     *  thenAnswer : mock 객체의 메서드를 직접 구현
     *  - 인자로 함수형 인터페이스 Answer를 전달하여 메서드 호출 시 answer 메서드 실행
     *  - InvocationOnMock 객체를 인자로 받아 메서드 호출 정보를 확인하거나 반환값을 지정할 수 있음
     *  - 마찬가지로 OnGoingStubbing 객체를 반환하여 체이닝 가능
     *
     *  InvocationOnMock
     *  - 메서드 호출 정보를 제공하는 인터페이스
     *  - getMock() : Mock 객체 반환
     *  - getMethod() : 호출된 메서드 반환 (Java Reflection)
     *  - getArguments() : 메서드 호출 시 전달된 인자 반환
     *  - callRealMethod() : 실제 메서드 호출
     */
    @Test
    void mockStubbingAnswerTest() {

        DummyService dummyService = mock();

        when(dummyService.dummyMethodWithParam("hello"))
                .thenAnswer(invocation -> "mock " + invocation.getArgument(0))
                .thenAnswer(invocation -> {
                    if (invocation.getArgument(0).equals("hello")) {
                        throw new IllegalArgumentException("mock exception");
                    }
                    return "mock " + invocation.getArgument(0);
                })
                .thenAnswer(InvocationOnMock::callRealMethod);

        assertEquals("mock hello", dummyService.dummyMethodWithParam("hello"));
        assertThrows(IllegalArgumentException.class, () -> dummyService.dummyMethodWithParam("hello"));
        assertEquals("dummy hello", dummyService.dummyMethodWithParam("hello"));
    }

    /**
     *  do-when 패턴
     *  - do* : 메서드의 행동을 지정, Stubber를 반환하여 체이닝 가능
     *  - when : Mock 객체를 인자로 받은 후 체이닝을 통해 메서드 호출을 지정
     *  - void 메서드를 Stubbing할 때 사용 (when-then 패턴에서는 void 메서드 Stubbing 불가)
     */
    @Test
    void mockStubbingDoTest() {

        DummyService dummyService = mock();

        doReturn("mock dummy")
                .doThrow(NullPointerException.class)
                .when(dummyService).getDummy();

        assertEquals("mock dummy", dummyService.getDummy());
        assertThrows(NullPointerException.class, dummyService::getDummy);
    }

    @Test
    void mockStubbingVoidTest() {

        DummyService dummyService = mock();

        doNothing()
                .doThrow(NullPointerException.class)
                .doAnswer(invocation -> {
                    log.info("mock void method");
                    return null;
                })
                .when(dummyService).dummyMethod();

        dummyService.dummyMethod();
        assertThrows(NullPointerException.class, dummyService::dummyMethod);
        dummyService.dummyMethod();
    }
}
