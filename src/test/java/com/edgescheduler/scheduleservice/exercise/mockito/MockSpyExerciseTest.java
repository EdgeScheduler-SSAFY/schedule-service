package com.edgescheduler.scheduleservice.exercise.mockito;

import com.edgescheduler.scheduleservice.service.DummyService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Spy : Mock 객체의 일부 메서드만 Stubbing하고 싶을 때 사용. Stubbing하지 않은 메서드는 실제 메서드를 호출
 */
public class MockSpyExerciseTest {

    /**
     * Spy 객체 생성
     * - Mock 객체와 마찬가지로 인자를 전달하지 않거나 Class 타입을 인자로 전달하여 Spy 객체 생성
     * - Stubbing하지 않은 메서드에 한해 실제 메서드를 호출하므로 생성하려는 객체는 반드시 기본 생성자를 가져야 함
     * - mocking하려는 클래스의 실제 인스턴스를 인자로 전달하여 기존 객체를 wrapping하는 Spy 객체 생성 가능
     */
    @Test
    void spyCreationTest() {
        DummyService spy1 = spy();
        DummyService spy2 = spy(DummyService.class);
        DummyService dummyService = new DummyService();
        DummyService spy3 = spy(dummyService);

        assertInstanceOf(DummyService.class, spy1);
        assertInstanceOf(DummyService.class, spy2);
        assertInstanceOf(DummyService.class, spy3);
    }

    @Test
    void spyDefaultBehaviorTest() {

        DummyService spy = spy();

        // Stubbing하지 않은 메서드는 실제 메서드를 호출
        assertEquals("dummy", spy.getDummy());

        // Stubbing한 메서드는 Stubbing한 값 반환
        when(spy.getDummy()).thenReturn("stubbing");
        assertEquals("stubbing", spy.getDummy());

        // Stubbing에 관계 없이 호출 정보는 함께 기록됨
        verify(spy, times(2)).getDummy();
    }
}
