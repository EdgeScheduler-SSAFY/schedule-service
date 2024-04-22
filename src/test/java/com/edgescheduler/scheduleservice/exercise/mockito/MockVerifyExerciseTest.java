package com.edgescheduler.scheduleservice.exercise.mockito;

import com.edgescheduler.scheduleservice.exercise.extension.TestToFail;
import com.edgescheduler.scheduleservice.exercise.extension.TestToFailExtension;
import com.edgescheduler.scheduleservice.exercise.extension.UnintendedSuccessException;
import com.edgescheduler.scheduleservice.service.DummyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.*;

@ExtendWith(TestToFailExtension.class)
public class MockVerifyExerciseTest {

    /**
     *  verify : Mock 객체의 메서드 실행과 관련된 검증을 수행
     *  - 메서드가 지정된 횟수만큼 호출되었는지 검증
     *  - 메서드에 전달된 인자를 ArgumentMatcher를 사용해 검증
     *
     *  verify(T mock) / verify(T mock, VerificationMode mode)
     *  - VerificationMode를 통해 호출 횟수를 지정. 주어지지 않으면 1회 호출(`times(1)`)을 기본값으로 사용
     *
     *  VerificationMode
     *  - times(int wantedNumberOfInvocations) : 지정된 횟수만큼 호출되었는지 검증
     *  - never() : 한 번도 호출되지 않았는지 검증
     *  - atLeast(int minNumberOfInvocations) : 최소 호출 횟수를 지정해 검증
     *  - atMost(int maxNumberOfInvocations) : 최대 호출 횟수를 지정해 검증
     *  - atLeastOnce() : 최소 한 번 이상 호출되었는지 검증
     */
    @Test
    void mockitoVerifyTest() {
        // given
        DummyService dummyService = mock(DummyService.class);

        // when
        dummyService.dummyMethod();
        dummyService.dummyMethodWithParam("test");
        dummyService.dummyMethodWithParam("test");
        dummyService.dummyMethodWithParamInt(1);
        dummyService.dummyMethodWithParamInt(2);
        dummyService.dummyMethodWithParamInt(3);

        // then
        verify(dummyService).dummyMethod();
        verify(dummyService, never()).getDummy();
        verify(dummyService, atLeast(1)).dummyMethodWithParam("test");
        verify(dummyService, atLeastOnce()).dummyMethodWithParam(argThat(arg -> arg.equals("test")));
        verify(dummyService, atMostOnce()).dummyMethodWithParamInt(1);
        verify(dummyService, atMost(3)).dummyMethodWithParamInt(anyInt());
    }

    /**
     *  VerificationMode
     *  - calls(int wantedNumberOfInvocations) : 지정된 횟수만큼 호출되었는지 검증, InOrder 객체와 함께 사용하여 호출 순서를 검증
     *
     *  InOrder
     *  - inOrder(T... mocks) : Mock 객체의 호출 순서를 검증
     */
    @TestToFail
    void mockitoVerifyInOrderFailTest() {
        // given
        DummyService dummyService = mock(DummyService.class);

        // when
        dummyService.dummyMethod();
        dummyService.dummyMethod();
        dummyService.dummyMethodWithParam("test");
        dummyService.dummyMethod();

        // then
        InOrder inOrder = inOrder(dummyService);
        inOrder.verify(dummyService, calls(3)).dummyMethod();
        inOrder.verify(dummyService).dummyMethodWithParam("test");

        throw new UnintendedSuccessException();
    }

    @Test
    void mockitoVerifyInOrderTest() {
        // given
        DummyService dummyService1 = mock(DummyService.class);
        DummyService dummyService2 = mock(DummyService.class);

        // when
        dummyService1.dummyMethod();
        dummyService1.dummyMethod();
        dummyService2.dummyMethod();
        dummyService1.dummyMethodWithParam("test");
        dummyService1.dummyMethod();
        dummyService2.getDummy();

        // then
        InOrder inOrder = inOrder(dummyService1, dummyService2);
        inOrder.verify(dummyService1, calls(2)).dummyMethod();
        inOrder.verify(dummyService2).dummyMethod();
        inOrder.verify(dummyService1).dummyMethodWithParam("test");
        inOrder.verify(dummyService1, calls(1)).dummyMethod();
        inOrder.verify(dummyService2).getDummy();
    }

    /**
     *  VerificationMode
     *  - only() : Mock 객체를 통해 호출된 메서드가 지정된 메서드가 유일한지, 해당 메서드를 1번만 호출하였는지 검증
     *
     *  verifyNoInteractions(T... mocks) : Mock 객체를 통해 호출된 메서드가 없는지 검증
     *  verifyNoMoreInteractions(T... mocks) : Mock 객체를 통해 호출된 메서드가 지정된 메서드 이외에 다른 메서드를 호출하지 않았는지 검증
     */
    @TestToFail
    void mockitoVerifyOnlyFailTest() {
        // given
        DummyService dummyService = mock(DummyService.class);

        // when
        dummyService.dummyMethod();
        dummyService.dummyMethodWithParam("test");

        // then
        verify(dummyService, only()).dummyMethod();

        throw new UnintendedSuccessException();
    }

    @TestToFail
    void mockitoVerifyNoInteractionsFailTest() {
        // given
        DummyService dummyService = mock(DummyService.class);

        // when
        dummyService.dummyMethod();

        // then
        verifyNoInteractions(dummyService);

        throw new UnintendedSuccessException();
    }

    @Test
    void mockitoVerifyNoMoreInteractionsFailTest() {
        // given
        DummyService dummyService = mock(DummyService.class);

        // when
        dummyService.dummyMethod();
        dummyService.dummyMethodWithParam("test");

        // then
        verify(dummyService).dummyMethod();
        verify(dummyService).dummyMethodWithParam(anyString());
        verifyNoMoreInteractions(dummyService);
    }

    /**
     *  timeout(long millis) : Mock 객체의 메서드 호출이 지정된 시간 내에 완료되었는지 검증
     *  after(long millis) : Mock 객체의 메서드 호출이 지정된 시간 이후에 완료되었는지 검증
     *  - 비동기 논블로킹으로 동작하는 메서드의 호출을 검증할 때 유용
     *
     *  timeout vs after
     *  - timeout : 지정된 시간이 다 지나기 전에 검증에 성공하면 즉시 통과 처리
     *  - after : 지정된 시간을 기다렸다가 검증 처리
     */
    @Test
    void mockitoVerifyTimeoutTest() {
        // given
        DummyService dummyService = mock(DummyService.class);

        // when
        dummyService.dummyMethod();

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            dummyService.dummyMethod();
            dummyService.dummyMethod();
        });

        // then
        verify(dummyService, times(1)).dummyMethod();   // 이 시점에는 1회밖에 호출되지 않음
        verify(dummyService, timeout(150).times(3)).dummyMethod();
    }

    @Test
    void mockitoVerifyAfterTest() {
        // given
        DummyService dummyService = mock(DummyService.class);

        // when
        dummyService.dummyMethod();

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            dummyService.dummyMethod();
            dummyService.dummyMethod();
        });

        // then
        verify(dummyService, times(1)).dummyMethod();
        verify(dummyService, after(150).times(3)).dummyMethod();
    }

    /**
     *  ArgumentCaptor<T>
     *  - Mock 객체의 메서드 호출 시 전달된 인자를 캡처
     *  - 캡처한 인자를 통해 추가적인 검증 수행 가능
     *  - 메서드를 여러 번 호출할 경우 전달된 인자들을 내부 리스트에 저장
     *
     *  captor.getValue() : 캡처한 인자 중 가장 최근에 전달된 인자 반환
     *  captor.getAllValues() : 캡처한 모든 인자의 리스트 반환
     */
    @Test
    void mockitoVerifyArgumentCaptorTest() {
        // given
        DummyService dummyService = mock(DummyService.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        // when
        dummyService.dummyMethodWithParam("question");
        dummyService.dummyMethodWithParam("answer");
        dummyService.dummyMethodWithParam("test");

        // then
        verify(dummyService, times(3)).dummyMethodWithParam(captor.capture());
        var expectedValues = List.of("question", "answer", "test");
        assertIterableEquals(expectedValues, captor.getAllValues());
    }
}
