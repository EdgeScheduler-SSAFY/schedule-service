package com.edgescheduler.scheduleservice.domain;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Converter
class RepeatDaySetConverter implements AttributeConverter<Set<String>, String> {

    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return String.join(",", attribute);
    }

    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(dbData.split(",")));
    }
}

