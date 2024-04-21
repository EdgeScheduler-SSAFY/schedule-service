package com.edgescheduler.scheduleservice.exercise.extension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class TestToFailExtension implements TestExecutionExceptionHandler{

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {

        // @TestToFail 어노테이션이 붙은 테스트 메서드가 실행될 때 예외가 발생하면, 예외를 다시 던지지 않고 테스트를 성공으로 처리
        if (context.getTestMethod().isPresent() &&
            context.getTestMethod().get().isAnnotationPresent(TestToFail.class) &&
            !(throwable instanceof UnintendedSuccessException)) {
            return;
        }

        // @TestToFail 어노테이션이 붙지 않은 경우, 실패로 처리
        throw throwable;
    }
}
