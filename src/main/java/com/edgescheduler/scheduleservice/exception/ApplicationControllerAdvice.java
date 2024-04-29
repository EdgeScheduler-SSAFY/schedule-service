package com.edgescheduler.scheduleservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApplicationControllerAdvice {

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException e) {
        return ResponseEntity
            .status(e.getHttpStatus())
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    public record ErrorResponse(String code, String message) {

    }
}
