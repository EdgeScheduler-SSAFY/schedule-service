package com.edgescheduler.scheduleservice.config.deserializer;

import com.edgescheduler.scheduleservice.message.ChangeTimeZoneMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;

public class ChangeTimeZoneMessageDeserializer implements Deserializer<ChangeTimeZoneMessage> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public ChangeTimeZoneMessage deserialize(String topic, byte[] data) {

        if (data == null) {
            return null;
        }

        try {
            return objectMapper.readValue(data, ChangeTimeZoneMessage.class);
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing ChangeTimeZoneMessage", e);
        }
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
