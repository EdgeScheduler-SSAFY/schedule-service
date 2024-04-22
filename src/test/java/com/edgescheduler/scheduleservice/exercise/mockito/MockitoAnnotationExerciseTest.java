package com.edgescheduler.scheduleservice.exercise.mockito;

import com.edgescheduler.scheduleservice.service.DummyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *  테스트 코드마다 Mock, Spy 객체를 생성하는 것은 번거로운 일이다.
 *  Mockito에서 제공하는 어노테이션을 사용하면 테스트 클래스 내에서 필요한 Mock, Spy, ArgumentCaptor 등을 쉽게 생성하고 주입할 수 있다.
 *
 *  이를 활용하기 위해서는 `MockitoAnnotations.openMocks(this)` 메서드를 사용하여 테스트 클래스 내에서 Mockito 어노테이션을 사용할 수 있도록 초기화해야 한다.
 *  MockitoExtension을 사용하면 테스트 클래스에 @ExtendWith(MockitoExtension.class) 어노테이션을 추가하여 위 코드를 대체할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
public class MockitoAnnotationExerciseTest {

    /**
     *  @Mock / @Spy : 어노테이션을 지정한 클래스를 기반으로 Mock, Spy 객체를 생성
     *  @Captor : ArgumentCaptor 객체를 생성
     *  @InjectMocks : Mock 객체를 주입받는 객체를 생성. 주입받는 클래스의 Mock / Spy 객체를 탐색하여 주입.
     */

    @Spy
    private DummyService spyDummyService;

//    @Mock
//    private DummyService mockDummyService;

    @InjectMocks
    private DummyInjectService dummyInjectService;

    @Test
    void mockitoInjectMocksTest() {

        String dummy = dummyInjectService.getDummy();

        assertEquals("dummy", dummyInjectService.getDummy());
        assertEquals("other dummy", dummyInjectService.getOtherDummy());
    }

    static class DummyInjectService {

        private final DummyService dummyService;

        public DummyInjectService(DummyService dummyService) {
            this.dummyService = dummyService;
        }

        public String getDummy() {
            return dummyService.getDummy();
        }

        public String getOtherDummy() {
            return "other dummy";
        }
    }
}
