package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.domain.MemberTimezone;
import com.edgescheduler.scheduleservice.message.ChangeTimeZoneMessage;
import com.edgescheduler.scheduleservice.repository.MemberTimezoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTimezoneService {

    private final MemberTimezoneRepository memberTimezoneRepository;

    @KafkaListener(topics = "${kafka.topic.timezone-configured}")
    public void listen(ChangeTimeZoneMessage message) {
        log.info("Received message: {} | {}", message.getMemberId(), message.getZoneId());
        memberTimezoneRepository.findById(message.getMemberId())
            .ifPresentOrElse(
                memberTimezone -> {
                    memberTimezone.changeZoneId(message.getZoneId());
                    memberTimezoneRepository.save(memberTimezone);
                },
                () -> memberTimezoneRepository.save(
                    MemberTimezone.builder()
                        .id(message.getMemberId())
                        .zoneId(message.getZoneId())
                        .build()
                )
            );
    }
}
