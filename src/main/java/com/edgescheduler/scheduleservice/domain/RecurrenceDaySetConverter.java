package com.edgescheduler.scheduleservice.domain;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Converter
class RecurrenceDaySetConverter implements AttributeConverter<EnumSet<RecurrenceDayType>, String> {

    private static final List<RecurrenceDayType> DESIRED_ORDER = Arrays.asList(
        RecurrenceDayType.MON, RecurrenceDayType.TUE, RecurrenceDayType.WED,
        RecurrenceDayType.THU, RecurrenceDayType.FRI, RecurrenceDayType.SAT,
        RecurrenceDayType.SUN
    );

    private static final Comparator<RecurrenceDayType> ORDERED_COMPARATOR = Comparator.comparingInt(
        DESIRED_ORDER::indexOf);

    @Override
    public String convertToDatabaseColumn(EnumSet<RecurrenceDayType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        // 지정된 순서대로 정렬
        List<String> sortedList = attribute.stream()
            .sorted(ORDERED_COMPARATOR)
            .map(Enum::name)
            .collect(Collectors.toList());
        return String.join(",", sortedList);
    }

    @Override
    public EnumSet<RecurrenceDayType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return EnumSet.noneOf(RecurrenceDayType.class);
        }

        // 지정된 순서로 정렬된 Set 생성
        Set<RecurrenceDayType> sortedSet = new TreeSet<>(ORDERED_COMPARATOR);
        Arrays.stream(dbData.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .map(RecurrenceDayType::valueOf)
            .forEach(sortedSet::add);

        return EnumSet.copyOf(sortedSet);
    }
}

