package com.edgescheduler.scheduleservice.service;

import org.springframework.stereotype.Service;

@Service
public class DummyService {

    public String getDummy() {
        return "dummy";
    }

    public Integer getDummyInt() {
        return 1;
    }

    public void dummyMethod() {
        System.out.println("dummy method");
    }

    public String dummyMethodWithParam(String param) {
        return "dummy " + param;
    }

    public Integer dummyMethodWithParamInt(Integer param) {
        return 1 + param;
    }
}
