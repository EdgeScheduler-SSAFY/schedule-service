package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.repository.SimpleScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SimpleScheduleServiceTest {

    @Mock
    private SimpleScheduleRepository scheduleRepository;

    @InjectMocks
    private SimpleScheduleService simpleScheduleService;



    @Test
    void calculateAvailability() {
    }
}