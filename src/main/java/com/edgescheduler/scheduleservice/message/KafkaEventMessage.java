package com.edgescheduler.scheduleservice.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
public class KafkaEventMessage {

    private LocalDateTime occurredAt;
}
