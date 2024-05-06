package com.edgescheduler.scheduleservice.config;

import com.edgescheduler.scheduleservice.message.KafkaEventMessage;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, KafkaEventMessage> producerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(null));
    }

    @Bean
    public KafkaTemplate<String, KafkaEventMessage> kafkaTemplate(KafkaProperties properties) {
        return new KafkaTemplate<>(producerFactory(properties));
    }
}
