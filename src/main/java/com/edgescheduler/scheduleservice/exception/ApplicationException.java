package com.edgescheduler.scheduleservice.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ApplicationException extends RuntimeException {

    private HttpStatus httpStatus;
    private String code;
    private String message;
}