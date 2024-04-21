package com.edgescheduler.scheduleservice.exercise.junit;

import org.junit.jupiter.api.*;

public class JUnitLifeCycleExerciseTest {

    /**
     *  @Test annotation
     *  - 테스트 메서드 지정
     *  - 메서드는 void 타입이어야 하며, 파라미터를 가질 수 없음
     *  - 테스트 클래스 내부의 테스트 메서드 사이에는 순서 보장이 되지 않음
     *  - 메서드 접근 제어자는 public일 필요는 없지만, private이 붙으면 안됨 -> 일반적으로 JUnit 에서는 public 생략하는 것을 권장
     */
    @Test
    void test1() {
        System.out.println("test1");
    }

    /**
     *  @BeforeAll annotation : 모든 테스트 메서드가 실행되기 전 딱 한 번 실행
     *  @AfterAll annotation : 모든 테스트 메서드가 실행된 후 딱 한 번 실행
     *  static 메서드로 선언해야 함
     */
    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAll");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("afterAll");
    }

    /**
     *  @BeforeEach annotation
     *  - 각 테스트 메서드가 실행되기 전 실행
     *  - 테스트 데이터를 설정하고 테스트 환경을 준비
     *  @AfterEach annotation :
     *  - 각 테스트 메서드가 실행된 후 실행
     *  - 테스트 데이터를 정리하고 리소스 해제
     *
     *  하나의 테스트 클래스에 여러 @BeforeXX, @AfterXX 메서드를 선언할 수 있지만 순서가 보장되지 않으므로 주의
     *  BeforeAll - BeforeEach - Test - AfterEach - AfterAll 순으로 실행
     */
    @BeforeEach
    void beforeEach() {
        System.out.println("beforeEach");
    }

    @AfterEach
    void afterEach() {
        System.out.println("afterEach");
    }

    @Test
    void test2() {
        System.out.println("test2");
    }

    @Test
    void test3() {
        System.out.println("test3");
    }
}
