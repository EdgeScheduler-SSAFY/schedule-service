package com.edgescheduler.scheduleservice.controller;

import com.edgescheduler.scheduleservice.message.MeetingCreateMessage;
import com.edgescheduler.scheduleservice.service.KafkaProducer;
import java.time.LocalDateTime;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final KafkaProducer kafkaProducer;

    @PostMapping("/test")
    public void test() {
        MeetingCreateMessage meetingCreateMessage = MeetingCreateMessage.builder()
            .occurredAt(LocalDateTime.now())
            .scheduleId(1L)
            .scheduleName("Meeting")
            .organizerId(2)
            .organizerName("Organizer")
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now().plusHours(1))
            .runningTime(60)
            .attendeeIds(IntStream.range(1, 6).boxed().toList())
            .build();
        kafkaProducer.send("meeting-created", meetingCreateMessage);
    }
}
