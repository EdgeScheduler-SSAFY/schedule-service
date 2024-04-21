package com.edgescheduler.scheduleservice.exercise.mockito;

import com.edgescheduler.scheduleservice.service.DummyService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Slf4j
public class MockCreationExerciseTest {

    /**
     *  Mocking : 테스트 대상 객체를 직접 사용하지 않고 가짜 객체를 대신 사용하는 것
     *  인자를 전달하지 않거나 mocking할 클래스를 전달하여 Mock 객체 생성
     *  ex)
     *    DummyService dummyService1 = mock();                      -- 1
     *    DummyService dummyService2 = mock(DummyService.class);    -- 2
     *
     *  1의 경우, 인자를 전달하지 않아 mock() 메서드를 호출하는 것으로 보이지만 내부적으로는 가변인자를 받는 mock(T... reified)를 호출하고 있다.
     *  이는 제네릭을 통해 mocking하려는 클래스나 인터페이스를 감지하는 방식으로, 클래스 인자를 넘기는 것이 아니라면 인자를 전달하면 안된다.
     */

    @Test
    void mockCreationTest() {
        DummyService dummyService = mock();
        assertInstanceOf(DummyService.class, dummyService);
    }

    /**
     *  Mock 객체는 대상 객체의 모든 메서드를 구현한다.
     *  Stubbing을 하지 않은 경우
     *  - void 메서드 : 아무 동작도 하지 않음
     *  - primitive 타입 (Integer 등의 Wrapper class 포함): 기본값
     *  - 객체 타입 : null
     */

    @Test
    void mockDefaultBehaviorTest() {
        DummyService dummyService = mock();
        assertNull(dummyService.getDummy());
        assertEquals(0, dummyService.getDummyInt());
    }
}
