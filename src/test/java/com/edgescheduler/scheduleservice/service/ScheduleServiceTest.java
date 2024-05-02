package com.edgescheduler.scheduleservice.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.edgescheduler.scheduleservice.domain.Attendee;
import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import com.edgescheduler.scheduleservice.domain.MemberTimezone;
import com.edgescheduler.scheduleservice.domain.Proposal;
import com.edgescheduler.scheduleservice.domain.Recurrence;
import com.edgescheduler.scheduleservice.domain.RecurrenceFreqType;
import com.edgescheduler.scheduleservice.domain.Schedule;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest.RecurrenceDetails;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest.ScheduleAttendee;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.exception.ApplicationException;
import com.edgescheduler.scheduleservice.repository.AttendeeRepository;
import com.edgescheduler.scheduleservice.repository.MemberTimezoneRepository;
import com.edgescheduler.scheduleservice.repository.RecurrenceRepository;
import com.edgescheduler.scheduleservice.repository.ScheduleRepository;
import com.edgescheduler.scheduleservice.util.AlterTimeUtils;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
public class ScheduleServiceTest {

    @Autowired
    private SimpleScheduleService simpleScheduleService;

    @Autowired
    private MemberTimezoneRepository memberTimezoneRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private RecurrenceRepository recurrenceRepository;

    @DisplayName("반복 일정 등록")
    @Test
    void createRecurrenceScheduleTest() {

        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        RecurrenceDetails recurrenceDetails = RecurrenceDetails.builder()
            .freq("WEEKLY")
            .intv(2)
            .expiredDate(LocalDateTime.of(2024, 6, 1, 0, 0))
            .recurrenceDay(List.of("TUE", "MON"))
            .build();

        ScheduleCreateRequest recurrenceScheduleCreateRequest = ScheduleCreateRequest.builder()
            .organizerId(1)
            .name("일정1")
            .description("반복일정 등록 테스트")
            .type(ScheduleType.PERSONAL)
            .color(1)
            .startDatetime(LocalDateTime.of(2024, 5, 1, 9, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 1, 10, 0))
            .isPublic(false)
            .isRecurrence(true)
            .recurrence(recurrenceDetails)
            .build();

        var result = simpleScheduleService.createSchedule(recurrenceScheduleCreateRequest);
        Long scheduleId = result.getScheduleId();

        Schedule savedSchedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("null"));
        Instant startInstant = LocalDateTime.of(2024, 5, 1, 9, 0).atZone(ZoneId.of("Asia/Seoul"))
            .withZoneSameInstant(ZoneOffset.UTC).toInstant();
        Instant endInstant = LocalDateTime.of(2024, 5, 1, 10, 0).atZone(ZoneId.of("Asia/Seoul"))
            .withZoneSameInstant(ZoneOffset.UTC).toInstant();
        Instant expiredInstant = LocalDateTime.of(2024, 6, 1, 0, 0).atZone(ZoneId.of("Asia/Seoul"))
            .withZoneSameInstant(ZoneOffset.UTC).toInstant();

        assertAll(
            () -> assertEquals(recurrenceScheduleCreateRequest.getOrganizerId(),
                savedSchedule.getOrganizerId()),
            () -> assertEquals(recurrenceScheduleCreateRequest.getName(), savedSchedule.getName()),
            () -> assertEquals(recurrenceScheduleCreateRequest.getDescription(),
                savedSchedule.getDescription()),
            () -> assertEquals(recurrenceScheduleCreateRequest.getType(), savedSchedule.getType()),
            () -> assertEquals(recurrenceScheduleCreateRequest.getColor(),
                savedSchedule.getColor()),
            () -> assertEquals(recurrenceScheduleCreateRequest.getIsPublic(),
                savedSchedule.getIsPublic()),
            () -> assertEquals(startInstant, savedSchedule.getStartDatetime()),
            () -> assertEquals(endInstant, savedSchedule.getEndDatetime()),
            () -> assertEquals(recurrenceDetails.getFreq(),
                String.valueOf(savedSchedule.getRecurrence().getFreq())),
            () -> assertEquals(recurrenceDetails.getIntv(),
                savedSchedule.getRecurrence().getIntv()),
            () -> assertEquals(recurrenceDetails.getCount(),
                savedSchedule.getRecurrence().getCount()),
            () -> assertEquals(expiredInstant, savedSchedule.getRecurrence().getExpiredDate()),
            () -> assertIterableEquals(recurrenceDetails.getRecurrenceDay(),
                savedSchedule.getRecurrence().getRecurrenceDay())
        );
    }

    @DisplayName("회의 일정 등록")
    @Test
    void createMeetingScheduleTest() {
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        ScheduleAttendee attendee1 = ScheduleAttendee.builder()
            .memberId(1)
            .isRequired(true)
            .build();

        ScheduleAttendee attendee2 = ScheduleAttendee.builder()
            .memberId(2)
            .isRequired(true)
            .build();

        ScheduleAttendee attendee3 = ScheduleAttendee.builder()
            .memberId(3)
            .isRequired(false)
            .build();

        List<ScheduleAttendee> attendeeList = List.of(attendee1, attendee2, attendee3);

        ScheduleCreateRequest recurrenceScheduleCreateRequest = ScheduleCreateRequest.builder()
            .organizerId(1)
            .name("회의")
            .description("회의 일정 등록 테스트")
            .type(ScheduleType.MEETING)
            .color(1)
            .startDatetime(LocalDateTime.of(2024, 5, 1, 9, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 1, 10, 0))
            .isPublic(false)
            .isRecurrence(false)
            .attendeeList(attendeeList)
            .build();

        var result = simpleScheduleService.createSchedule(recurrenceScheduleCreateRequest);
        Long scheduleId = result.getScheduleId();
        Schedule savedSchedule = scheduleRepository.findById(scheduleId).orElseThrow();
        List<Attendee> attendees = attendeeRepository.findBySchedule(
            scheduleRepository.findById(scheduleId).orElseThrow());

        assertAll(
            () -> assertEquals(attendeeList.size(), attendees.size()),
            () -> assertEquals(attendees.get(0).getSchedule().getId(), savedSchedule.getId()),
            () -> assertEquals(attendees.get(1).getSchedule().getId(), savedSchedule.getId()),
            () -> assertEquals(attendees.get(2).getSchedule().getId(), savedSchedule.getId())
        );
    }

    @DisplayName("일정 상세 조회")
    @Test
    void getSchedule() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());
        // ZoneId
        ZoneId zoneId = ZoneId.of(memberTimezoneRepository.findById(1).orElseThrow().getZoneId());
        // schedule
        Schedule schedule = Schedule.builder()
            .organizerId(1)
            .name("회의")
            .description("회의 설명")
            .type(ScheduleType.MEETING)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 14, 0), zoneId))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 15, 0), zoneId))
            .isPublic(true)
            .color(3)
            .build();
        // 참여자
        Attendee attendee1 = Attendee.builder()
            .schedule(schedule)
            .memberId(2)
            .isRequired(true)
            .status(AttendeeStatus.PENDING)
            .build();

        Attendee attendee2 = Attendee.builder()
            .schedule(schedule)
            .memberId(3)
            .isRequired(false)
            .status(AttendeeStatus.PENDING)
            .build();

        Attendee attendee3 = Attendee.builder()
            .schedule(schedule)
            .memberId(3)
            .isRequired(true)
            .status(AttendeeStatus.DECLINED)
            .reason("시간이 안돼서")
            .proposal(Proposal.builder()
                .startDatetime(
                    AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 11, 0),
                        ZoneId.of("Asia/Seoul")))
                .endDatetime(
                    AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 12, 0),
                        ZoneId.of("Asia/Seoul")))
                .build())
            .build();

        List<Attendee> attendeeList = List.of(attendee1, attendee2, attendee3);
        schedule.setAttendees(attendeeList);
        Schedule savedSchedule = scheduleRepository.save(schedule);

        var result = simpleScheduleService.getSchedule(1, savedSchedule.getId());
        assertAll(
            () -> assertEquals(schedule.getName(), savedSchedule.getName()),
            () -> assertEquals(schedule.getEndDatetime(), savedSchedule.getEndDatetime()),
            () -> assertEquals(schedule.getStartDatetime(), savedSchedule.getStartDatetime()),
            () -> assertEquals(schedule.getDescription(), savedSchedule.getDescription()),
            () -> assertEquals(schedule.getColor(), savedSchedule.getColor()),
            () -> assertEquals(schedule.getIsPublic(), savedSchedule.getIsPublic()),
            () -> assertEquals(schedule.getOrganizerId(), savedSchedule.getOrganizerId()),
            () -> assertEquals(attendeeList.size(), result.getAttendeeList().size())
        );
    }

    @DisplayName("주최자가 아닌 유저는 수정 불가능한지 체크")
    @Test
    void isUpdateScheduleOnlyOrganizerTest() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(2)
            .zoneId("Asia/Seoul")
            .build());
        // 기존 일정 저장
        Schedule schedule = Schedule.builder()
            .organizerId(2)
            .name("수정 전 일정명")
            .description("수정 전 일정 내용")
            .type(ScheduleType.PERSONAL)
            .color(3)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 7, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 8, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(true)
            .recurrence(Recurrence.builder()
                .count(3)
                .build())
            .build();
        scheduleRepository.save(schedule);

        ScheduleUpdateRequest scheduleUpdateRequest = ScheduleUpdateRequest.builder()
            .name("수정 후 일정명")
            .description("수정 후 일정 내용")
            .type(ScheduleType.PERSONAL)
            .color(2)
            .startDatetime(LocalDateTime.of(2024, 5, 22, 8, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 22, 9, 0))
            .isPublic(false)
            .isRecurrence(true)
            .isOneOff(true)
            .recurrence(ScheduleUpdateRequest.RecurrenceDetails.builder()
                .count(5)
                .build())
            .build();

        Assertions.assertThrows(ApplicationException.class,
            () -> simpleScheduleService.updateSchedule(1, schedule.getId(), scheduleUpdateRequest));
    }

    @DisplayName("해당 일정만 수정하는 반복일정 테스트")
    @Test
    void updateRecurrenceScheduleByOneOff() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());
        // 기존 일정 저장
        Schedule schedule = Schedule.builder()
            .organizerId(1)
            .name("수정 전 일정명")
            .description("수정 전 일정내용")
            .type(ScheduleType.PERSONAL)
            .color(3)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 7, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 8, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(true)
            .recurrence(Recurrence.builder()
                .count(3)
                .build())
            .build();
        scheduleRepository.save(schedule);

        // 수정사항
        ScheduleUpdateRequest scheduleUpdateRequest = ScheduleUpdateRequest.builder()
            .name("수정 후 일정명")
            .description("수정 후 일정내용")
            .type(ScheduleType.PERSONAL)
            .color(2)
            .startDatetime(LocalDateTime.of(2024, 5, 22, 8, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 22, 9, 0))
            .isPublic(false)
            .isRecurrence(true)
            .isOneOff(true)
            .recurrence(ScheduleUpdateRequest.RecurrenceDetails.builder()
                .count(5)
                .build())
            .build();

        simpleScheduleService.updateSchedule(1, schedule.getId(), scheduleUpdateRequest);
        Schedule updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertAll(
            () -> assertEquals(schedule.getId(), updatedSchedule.getId()),
            () -> assertEquals(schedule.getName(), updatedSchedule.getName()),
            () -> assertEquals(schedule.getRecurrence().getCount(),
                updatedSchedule.getRecurrence().getCount()),
            () -> assertEquals(schedule.getRecurrence().getIntv(),
                updatedSchedule.getRecurrence().getIntv()),
            () -> assertEquals(schedule.getRecurrence().getExpiredDate(),
                updatedSchedule.getRecurrence().getExpiredDate()),
            () -> assertSame(schedule.getRecurrence().getFreq(),
                updatedSchedule.getRecurrence().getFreq()),
            () -> assertEquals(schedule.getRecurrence().getRecurrenceDay(),
                updatedSchedule.getRecurrence().getRecurrenceDay()),
            () -> assertEquals(schedule.getColor(), updatedSchedule.getColor()),
            () -> assertEquals(schedule.getDescription(), updatedSchedule.getDescription()),
            () -> assertEquals(schedule.getIsPublic(), updatedSchedule.getIsPublic())
        );
    }

    @DisplayName("모든 반복 일정을 수정하는 테스트")
    @Test
    void updateAllRecurrenceSchedule() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());
        // 기존 일정 저장
        Recurrence recurrence = Recurrence.builder()
            .freq(RecurrenceFreqType.WEEKLY)
            .intv(2)
            .expiredDate(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 12, 1, 0, 0),
                    ZoneId.of("Asia/Seoul")))
            .build();
        Schedule schedule = Schedule.builder()
            .organizerId(1)
            .name("수정 전 일정명")
            .description("수정 전 일정내용")
            .type(ScheduleType.PERSONAL)
            .color(3)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 7, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 8, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(true)
            .recurrence(recurrence)
            .build();
        scheduleRepository.save(schedule);

        // 수정사항
        ScheduleUpdateRequest scheduleUpdateRequest = ScheduleUpdateRequest.builder()
            .name("수정 후 일정명")
            .description("수정 후 일정 내용")
            .type(ScheduleType.PERSONAL)
            .color(2)
            .startDatetime(LocalDateTime.of(2024, 5, 22, 8, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 22, 9, 0))
            .isPublic(false)
            .isRecurrence(true)
            .isOneOff(false)
            .recurrence(ScheduleUpdateRequest.RecurrenceDetails.builder()
                .count(5)
                .build())
            .build();

        simpleScheduleService.updateSchedule(1, schedule.getId(), scheduleUpdateRequest);

        Schedule updatedSchedule = scheduleRepository.findById(schedule.getId() + 1).orElseThrow();
        Schedule originSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertAll(
            // 기존 반복 테이블에서 만료기한 수정 여부 체크
            () -> assertEquals(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .withZoneSameInstant(ZoneOffset.UTC).toInstant(),
                originSchedule.getRecurrence().getExpiredDate()),
            () -> assertEquals(scheduleUpdateRequest.getName(), updatedSchedule.getName()),
            () -> assertEquals(scheduleUpdateRequest.getDescription(),
                updatedSchedule.getDescription()),
            () -> assertEquals(scheduleUpdateRequest.getColor(), updatedSchedule.getColor()),
            () -> assertEquals(scheduleUpdateRequest.getType(), updatedSchedule.getType()),
            () -> assertEquals(scheduleUpdateRequest.getIsPublic(), updatedSchedule.getIsPublic()),
            () -> assertEquals(scheduleUpdateRequest.getRecurrence().getCount(),
                updatedSchedule.getRecurrence().getCount()),
            () -> assertEquals(scheduleUpdateRequest.getRecurrence().getIntv(),
                updatedSchedule.getRecurrence().getIntv()),
            () -> assertEquals(scheduleUpdateRequest.getRecurrence().getFreq(),
                updatedSchedule.getRecurrence().getFreq() != null ? String.valueOf(
                    updatedSchedule.getRecurrence().getFreq()) : null),
            () -> assertEquals(scheduleUpdateRequest.getRecurrence().getExpiredDate() != null
                    ? AlterTimeUtils.LocalDateTimeToInstant(
                    scheduleUpdateRequest.getRecurrence().getExpiredDate(), ZoneId.of("Asia/Seoul"))
                    : null,
                updatedSchedule.getRecurrence().getExpiredDate()),
            () -> assertEquals(
                scheduleUpdateRequest.getRecurrence().getRecurrenceDay() != null ?
                    new HashSet<>(scheduleUpdateRequest.getRecurrence().getRecurrenceDay())
                    : new TreeSet<>(),
                updatedSchedule.getRecurrence().getRecurrenceDay())
        );

    }

    @DisplayName("반복하지 않는 일정 수정")
    @Test
    void updateNotRecurrenceSchedule() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        Schedule schedule = Schedule.builder()
            .organizerId(1)
            .name("수정 전 일정명")
            .description("수정 전 일정내용")
            .type(ScheduleType.PERSONAL)
            .color(3)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 7, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 8, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(false)
            .build();
        Schedule savedSchedule = scheduleRepository.save(schedule);

        // 수정사항
        ScheduleUpdateRequest scheduleUpdateRequest = ScheduleUpdateRequest.builder()
            .name("수정 후 일정명")
            .description("수정 후 일정 내용")
            .type(ScheduleType.PERSONAL)
            .color(2)
            .startDatetime(LocalDateTime.of(2024, 5, 22, 8, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 22, 9, 0))
            .isRecurrence(false)
            .isPublic(false)
            .isOneOff(false)
            .build();

        simpleScheduleService.updateSchedule(1, savedSchedule.getId(), scheduleUpdateRequest);
        Schedule updatedSchedule = scheduleRepository.findById(savedSchedule.getId()).orElseThrow();
        assertAll(
            () -> assertEquals(scheduleUpdateRequest.getName(), updatedSchedule.getName()),
            () -> assertEquals(scheduleUpdateRequest.getColor(), updatedSchedule.getColor()),
            () -> assertEquals(scheduleUpdateRequest.getDescription(),
                updatedSchedule.getDescription()),
            () -> assertEquals(scheduleUpdateRequest.getIsPublic(), updatedSchedule.getIsPublic()),
            () -> assertEquals(scheduleUpdateRequest.getType(), updatedSchedule.getType()),
            () -> assertEquals(
                AlterTimeUtils.LocalDateTimeToInstant(scheduleUpdateRequest.getStartDatetime(),
                    ZoneId.of("Asia/Seoul")), updatedSchedule.getStartDatetime()),
            () -> assertEquals(
                AlterTimeUtils.LocalDateTimeToInstant(scheduleUpdateRequest.getEndDatetime(),
                    ZoneId.of("Asia/Seoul")), updatedSchedule.getEndDatetime())
        );


    }

    @Transactional
    @DisplayName("회의 일정 수정")
    @Test
    void updateMeetingSchedule() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        Schedule schedule = Schedule.builder()
            .organizerId(1)
            .name("수정 전 회의명")
            .description("수정 전 회의 내용")
            .type(ScheduleType.MEETING)
            .color(3)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 7, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 8, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(true)
            .build();

        // 기존 참여자
        Attendee originAttendee1 = Attendee.builder()
            .schedule(schedule)
            .memberId(1)
            .isRequired(true)
            .status(AttendeeStatus.PENDING)
            .build();

        Attendee originAttendee2 = Attendee.builder()
            .schedule(schedule)
            .memberId(2)
            .isRequired(false)
            .status(AttendeeStatus.PENDING)
            .build();

        Attendee originAttendee3 = Attendee.builder()
            .schedule(schedule)
            .memberId(3)
            .isRequired(true)
            .status(AttendeeStatus.DECLINED)
            .reason("시간이 안돼서")
            .proposal(Proposal.builder()
                .startDatetime(
                    AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 11, 0),
                        ZoneId.of("Asia/Seoul")))
                .endDatetime(
                    AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 12, 0),
                        ZoneId.of("Asia/Seoul")))
                .build())
            .build();

        List<Attendee> originAttendeeList = List.of(originAttendee1, originAttendee2,
            originAttendee3);
        schedule.setAttendees(originAttendeeList);
        Schedule savedSchedule = scheduleRepository.save(schedule);

        // 변경된 참석자 리스트
        ScheduleUpdateRequest.ScheduleAttendee newAttendee1 = ScheduleUpdateRequest.ScheduleAttendee.builder()
            .memberId(1)
            .isRequired(true)
            .build();

        ScheduleUpdateRequest.ScheduleAttendee newAttendee2 = ScheduleUpdateRequest.ScheduleAttendee.builder()
            .memberId(3)
            .isRequired(true)
            .build();

        ScheduleUpdateRequest.ScheduleAttendee newAttendee3 = ScheduleUpdateRequest.ScheduleAttendee.builder()
            .memberId(4)
            .isRequired(true)
            .build();
        List<ScheduleUpdateRequest.ScheduleAttendee> newAttendeeList = List.of(newAttendee1,
            newAttendee2, newAttendee3);

        ScheduleUpdateRequest scheduleUpdateRequest = ScheduleUpdateRequest.builder()
            .name("수정 후 회의명")
            .description("수정 후 회의 내용")
            .type(ScheduleType.MEETING)
            .color(2)
            .startDatetime(LocalDateTime.of(2024, 5, 22, 8, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 22, 9, 0))
            .isPublic(true)
            .isRecurrence(false)
            .isOneOff(true)
            .attendeeList(newAttendeeList)
            .build();

        simpleScheduleService.updateSchedule(1, savedSchedule.getId(), scheduleUpdateRequest);
        Schedule updatedSchedule = scheduleRepository.findById(savedSchedule.getId()).orElseThrow();

        assertAll(
            () -> assertEquals(scheduleUpdateRequest.getName(), updatedSchedule.getName()),
            () -> assertEquals(scheduleUpdateRequest.getDescription(),
                updatedSchedule.getDescription()),
            () -> assertEquals(scheduleUpdateRequest.getType(), updatedSchedule.getType()),
            () -> assertEquals(scheduleUpdateRequest.getColor(), updatedSchedule.getColor()),
            () -> assertEquals(
                AlterTimeUtils.LocalDateTimeToInstant(scheduleUpdateRequest.getStartDatetime(),
                    ZoneId.of("Asia/Seoul")), updatedSchedule.getStartDatetime()),
            () -> assertEquals(
                AlterTimeUtils.LocalDateTimeToInstant(scheduleUpdateRequest.getEndDatetime(),
                    ZoneId.of("Asia/Seoul")), updatedSchedule.getEndDatetime()),
            () -> assertEquals(scheduleUpdateRequest.getIsPublic(), updatedSchedule.getIsPublic()),
            () -> assertEquals(newAttendeeList.size(),updatedSchedule.getAttendees().size())
        );
    }
}
