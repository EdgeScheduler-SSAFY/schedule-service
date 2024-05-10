package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.message.ChangeTimeZoneMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemberTimezoneService {

    @KafkaListener(topics = "${kafka.topic.timezone-configured}")
    public void listen(ChangeTimeZoneMessage message) {
        log.info("Received message: {} | {}", message.getMemberId(), message.getZoneId());
    }
}
