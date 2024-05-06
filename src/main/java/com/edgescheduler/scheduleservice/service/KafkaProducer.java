package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.message.KafkaEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, KafkaEventMessage> kafkaTemplate;

    public void send(String topic, KafkaEventMessage message) {
        kafkaTemplate.send(topic, message);
    }
}
