package com.edgescheduler.scheduleservice.exercise.junit;

import org.junit.jupiter.api.*;

public class JUnitNestedExerciseTest {

    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAllParent");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("afterAllParent");
    }

    @BeforeEach
    void beforeEach() {
        System.out.println("beforeEachParent");
    }

    @AfterEach
    void afterEach() {
        System.out.println("afterEachParent");
    }

    @Test
    void test1() {
        System.out.println("parent test 1");
    }

    /**
     *  @Nested annotation
     *  - 중첩 테스트 클래스를 선언
     *  - 테스트를 그룹화하고 구조화, 관계를 만들 수 있게 지원하는 어노테이션
     *  - Nested 클래스 내부에서 수명 주기 메서드(@BeforeXX, @AfterXX)를 별도로 지정할 수 있음
     *  - 부모 클래스의 수명 주기 메서드는 Nested 클래스에도 수행됨
     *  - 전체 테스트 클래스 실행 시 부모 클래스부터 Nested 클래스 순으로 순차적으로 수행
     */
    @Nested
    class NestedTest {

        @BeforeAll
        static void beforeAllNested() {
            System.out.println("beforeAllNested");
        }

        @AfterAll
        static void afterAllNested() {
            System.out.println("afterAllNested");
        }

        @BeforeEach
        void beforeEachNested() {
            System.out.println("beforeEachNested");
        }

        @AfterEach
        void afterEachNested() {
            System.out.println("afterEachNested");
        }

        @Test
        void nestedTest1() {
            System.out.println("nested test 1");
        }

        @Test
        void nestedTest2() {
            System.out.println("nested test 2");
        }

        @Nested
        class NestedNestedTest {

            @BeforeEach
            void beforeEachNestedNested() {
                System.out.println("beforeEachNestedNested");
            }

            @Test
            void nestedNestedTest1() {
                System.out.println("nested nested test 1");
            }
        }
    }

    /**
     *  @Nested annotation이 붙지 않은 클래스 내 테스트 메서드는 실행되지 않음
     */
    class NonNestedTest {

        @Test
        void nonNestedTest1() {
            System.out.println("non nested test 1");
        }
    }
}
