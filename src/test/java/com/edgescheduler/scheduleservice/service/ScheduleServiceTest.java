package com.edgescheduler.scheduleservice.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edgescheduler.scheduleservice.domain.Attendee;
import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import com.edgescheduler.scheduleservice.domain.MemberTimezone;
import com.edgescheduler.scheduleservice.domain.Proposal;
import com.edgescheduler.scheduleservice.domain.Recurrence;
import com.edgescheduler.scheduleservice.domain.RecurrenceDayType;
import com.edgescheduler.scheduleservice.domain.RecurrenceFreqType;
import com.edgescheduler.scheduleservice.domain.Schedule;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.request.ChangeScheduleTimeRequest;
import com.edgescheduler.scheduleservice.dto.request.DecideAttendanceRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest.RecurrenceDetails;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest.ScheduleAttendee;
import com.edgescheduler.scheduleservice.dto.request.ScheduleDeleteRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleDeleteRequest.ScheduleDeleteRange;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse.IndividualSchedule;
import com.edgescheduler.scheduleservice.dto.response.ScheduleUpdateResponse;
import com.edgescheduler.scheduleservice.exception.ApplicationException;
import com.edgescheduler.scheduleservice.repository.AttendeeRepository;
import com.edgescheduler.scheduleservice.repository.MemberTimezoneRepository;
import com.edgescheduler.scheduleservice.repository.ProposalRepository;
import com.edgescheduler.scheduleservice.repository.RecurrenceRepository;
import com.edgescheduler.scheduleservice.repository.ScheduleRepository;
import com.edgescheduler.scheduleservice.util.AlterTimeUtils;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
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
    @Autowired
    private ProposalRepository proposalRepository;

    @DisplayName("반복 일정 등록")
    @Test
    void createRecurrenceScheduleTest() {

        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());
        List<RecurrenceDayType> recurrenceDay = List.of(RecurrenceDayType.MON,
            RecurrenceDayType.TUE, RecurrenceDayType.WED, RecurrenceDayType.THU,
            RecurrenceDayType.FRI, RecurrenceDayType.SAT, RecurrenceDayType.SUN);
        RecurrenceDetails recurrenceDetails = RecurrenceDetails.builder()
            .freq("WEEKLY")
            .intv(2)
            .expiredDate(LocalDateTime.of(2024, 6, 1, 0, 0))
            .recurrenceDay(recurrenceDay)
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
            .orElseThrow();

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
            () -> assertEquals(startInstant, savedSchedule.getStartDatetime()),
            () -> assertEquals(endInstant, savedSchedule.getEndDatetime()),
            () -> assertEquals(recurrenceScheduleCreateRequest.getType(), savedSchedule.getType()),
            () -> assertEquals(recurrenceScheduleCreateRequest.getColor(),
                savedSchedule.getColor()),
            () -> assertEquals(recurrenceScheduleCreateRequest.getIsPublic(),
                savedSchedule.getIsPublic()),
            () -> assertEquals(recurrenceDetails.getFreq(),
                String.valueOf(savedSchedule.getRecurrence().getFreq())),
            () -> assertEquals(recurrenceDetails.getIntv(),
                savedSchedule.getRecurrence().getIntv()),
            () -> assertEquals(recurrenceDetails.getCount(),
                savedSchedule.getRecurrence().getCount()),
            () -> assertEquals(expiredInstant, savedSchedule.getRecurrence().getExpiredDate())
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
    void getScheduleTest() {
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
            .isDeleted(false)
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

        Proposal proposal = Proposal.builder()
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 11, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 12, 0),
                    ZoneId.of("Asia/Seoul")))
            .build();
        Proposal savedProposal = proposalRepository.save(proposal);
        Attendee attendee3 = Attendee.builder()
            .schedule(schedule)
            .memberId(3)
            .isRequired(true)
            .status(AttendeeStatus.DECLINED)
            .reason("시간이 안돼서")
            .proposal(savedProposal)
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
            .isDeleted(false)
            .recurrence(Recurrence.builder()
                .count(3)
                .freq(RecurrenceFreqType.DAILY)
                .intv(3)
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
    void updateRecurrenceScheduleByOneOffTest() {
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
            .isDeleted(false)
            .recurrence(Recurrence.builder()
                .count(3)
                .freq(RecurrenceFreqType.DAILY)
                .intv(4)
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
            .build();

        ScheduleUpdateResponse scheduleUpdateResponse = simpleScheduleService.updateSchedule(1,
            schedule.getId(), scheduleUpdateRequest);
        Schedule updatedSchedule = scheduleRepository.findById(
            scheduleUpdateResponse.getScheduleId()).orElseThrow();
        assertAll(
            () -> assertEquals(scheduleUpdateRequest.getName(), updatedSchedule.getName()),
            () -> assertEquals(scheduleUpdateRequest.getColor(), updatedSchedule.getColor()),
            () -> assertEquals(scheduleUpdateRequest.getDescription(),
                updatedSchedule.getDescription()),
            () -> assertEquals(scheduleUpdateRequest.getIsPublic(), updatedSchedule.getIsPublic()),
            () -> assertEquals(
                AlterTimeUtils.LocalDateTimeToInstant(scheduleUpdateRequest.getStartDatetime(),
                    ZoneId.of("Asia/Seoul")), updatedSchedule.getStartDatetime()),
            () -> assertEquals(
                AlterTimeUtils.LocalDateTimeToInstant(scheduleUpdateRequest.getEndDatetime(),
                    ZoneId.of("Asia/Seoul")), updatedSchedule.getEndDatetime(
                ))
        );
    }

    @DisplayName("모든 반복 일정을 수정하는 테스트")
    @Test
    void updateAllRecurrenceScheduleTest() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());
        // 기존 일정 저장
        Recurrence recurrence = Recurrence.builder()
            .freq(RecurrenceFreqType.WEEKLY)
            .intv(2)
            .recurrenceDay(EnumSet.of(RecurrenceDayType.FRI, RecurrenceDayType.TUE))
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
            .isDeleted(false)
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
                .freq("DAILY")
                .intv(5)
                .count(5)
                .build())
            .build();

        simpleScheduleService.updateSchedule(1, schedule.getId(), scheduleUpdateRequest);

        Schedule updatedSchedule = scheduleRepository.findById(schedule.getId() + 1).orElseThrow();
        Schedule originSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertAll(
            // 기존 반복 테이블에서 만료기한 수정 여부 체크
            () -> assertEquals(
                LocalDateTime.now().toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC),
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
    void updateNotRecurrenceScheduleTest() {
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
            .isDeleted(false)
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
    void updateMeetingScheduleTest() {
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
            .isDeleted(false)
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

        Proposal proposal = Proposal.builder()
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 11, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 12, 0),
                    ZoneId.of("Asia/Seoul")))
            .build();
        Proposal savedProposal  = proposalRepository.save(proposal);
        Attendee originAttendee3 = Attendee.builder()
            .schedule(schedule)
            .memberId(3)
            .isRequired(true)
            .status(AttendeeStatus.DECLINED)
            .reason("시간이 안돼서")
            .proposal(savedProposal)
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
            () -> assertEquals(newAttendeeList.size(), updatedSchedule.getAttendees().size())
        );
    }

    @DisplayName("회의 외 일정 모두 삭제")
    @Test
    void deleteAllScheduleExceptMeetingTest() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        Schedule notMeetingSchedule = Schedule.builder()
            .organizerId(1)
            .name("회의 외 일정")
            .description("회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .color(1)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .recurrence(Recurrence.builder()
                .count(3)
                .freq(RecurrenceFreqType.DAILY)
                .intv(7)
                .build())
            .isPublic(false)
            .isDeleted(false)
            .build();
        Schedule savedNotMeetingSchedule = scheduleRepository.save(notMeetingSchedule);

        ScheduleDeleteRequest scheduleDeleteRequest = ScheduleDeleteRequest.builder()
            .deleteRange(ScheduleDeleteRange.ALL)
            .build();

        simpleScheduleService.deleteSchedule(1, savedNotMeetingSchedule.getId(),
            scheduleDeleteRequest);

        assertAll(
            () -> assertTrue(
                scheduleRepository.findById(savedNotMeetingSchedule.getId()).isEmpty()),
            () -> assertTrue(
                recurrenceRepository.findById(savedNotMeetingSchedule.getRecurrence().getId())
                    .isEmpty())
        );
    }

    @DisplayName("회의 외 일정 선택 삭제")
    @Test
    void deleteSelectedScheduleExceptMeetingTest() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        Schedule notMeetingSchedule = Schedule.builder()
            .organizerId(1)
            .name("회의 외 일정")
            .description("회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .color(1)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .recurrence(Recurrence.builder()
                .freq(RecurrenceFreqType.WEEKLY)
                .intv(2)
                .recurrenceDay(EnumSet.of(RecurrenceDayType.FRI, RecurrenceDayType.TUE))
                .count(4)
                .build())
            .isPublic(false)
            .isDeleted(false)
            .build();
        Schedule savedNotMeetingSchedule = scheduleRepository.save(notMeetingSchedule);
        ScheduleDeleteRequest scheduleDeleteRequest = ScheduleDeleteRequest.builder()
            .deleteRange(ScheduleDeleteRange.ONE)
            .deleteDatetime(LocalDateTime.of(2024, 5, 8, 9, 0))
            .build();

        simpleScheduleService.deleteSchedule(1, savedNotMeetingSchedule.getId(),
            scheduleDeleteRequest);

        assertFalse(
            scheduleRepository.findScheduleByParentSchedule(savedNotMeetingSchedule).isEmpty());
    }

    @DisplayName("회의 외 일정 오늘부터 삭제")
    @Test
    void deleteScheduleExceptMeetingFromTodayTest() {
        // memberTimezone 저장
        MemberTimezone memberTimezone = memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        Schedule notMeetingSchedule = Schedule.builder()
            .organizerId(1)
            .name("회의 외 일정")
            .description("회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .color(1)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .recurrence(Recurrence.builder()
                .freq(RecurrenceFreqType.WEEKLY)
                .intv(2)
                .recurrenceDay(
                    EnumSet.of(RecurrenceDayType.FRI, RecurrenceDayType.TUE, RecurrenceDayType.WED))
                .count(6)
                .build())
            .isPublic(false)
            .isDeleted(false)
            .build();
        Schedule savedNotMeetingSchedule = scheduleRepository.save(notMeetingSchedule);
        ScheduleDeleteRequest scheduleDeleteRequest = ScheduleDeleteRequest.builder()
            .deleteRange(ScheduleDeleteRange.AFTERALL)
            .deleteDatetime(LocalDateTime.of(2024, 5, 8, 9, 0))
            .build();

        simpleScheduleService.deleteSchedule(1, savedNotMeetingSchedule.getId(),
            scheduleDeleteRequest);

        Recurrence recurrence = recurrenceRepository.findById(
            savedNotMeetingSchedule.getRecurrence().getId()).orElseThrow();
        assertEquals(
            scheduleDeleteRequest.getDeleteDatetime().toLocalDate().atStartOfDay()
                .toInstant(ZoneOffset.UTC),
            recurrence.getExpiredDate());
    }

    @DisplayName("회의 일정 삭제")
    @Test
    void deleteScheduleMeetingTest() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        Schedule meetingSchedule = Schedule.builder()
            .organizerId(1)
            .name("회의")
            .description("회의 일정")
            .type(ScheduleType.MEETING)
            .color(1)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(false)
            .isDeleted(false)
            .build();
        Schedule savedMeetingSchedule = scheduleRepository.save(meetingSchedule);

        ScheduleDeleteRequest scheduleDeleteRequest = ScheduleDeleteRequest.builder()
            .deleteRange(ScheduleDeleteRange.ALL)
            .build();

        simpleScheduleService.deleteSchedule(1, savedMeetingSchedule.getId(),
            scheduleDeleteRequest);

        assertTrue(scheduleRepository.findById(savedMeetingSchedule.getId()).isEmpty());
    }

    @DisplayName("반복 아닌 일정 기간별 조회")
    @Test
    void getSchedulesByPeriodTest() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(2)
            .zoneId("Europe/London")
            .build());

        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(3)
            .zoneId("Europe/Paris")
            .build());

        // 회의 일정 등록
        Schedule meetingSchedule1 = Schedule.builder()
            .organizerId(1)
            .name("회의")
            .description("회의 일정")
            .type(ScheduleType.MEETING)
            .color(1)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(true)
            .isDeleted(false)
            .build();

        Schedule meetingSchedule2 = Schedule.builder()
            .organizerId(2)
            .name("회의")
            .description("회의 일정")
            .type(ScheduleType.MEETING)
            .color(3)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 6, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 6, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(true)
            .isDeleted(false)
            .build();

        // 참석자
        Attendee attendee1 = Attendee.builder()
            .schedule(meetingSchedule1)
            .isRequired(true)
            .status(AttendeeStatus.ACCEPTED)
            .memberId(1)
            .build();

        Attendee attendee2 = Attendee.builder()
            .schedule(meetingSchedule1)
            .isRequired(true)
            .status(AttendeeStatus.PENDING)
            .memberId(2)
            .build();

        Attendee attendee3 = Attendee.builder()
            .schedule(meetingSchedule1)
            .isRequired(false)
            .status(AttendeeStatus.PENDING)
            .memberId(3)
            .build();

        Attendee attendee4 = Attendee.builder()
            .schedule(meetingSchedule2)
            .isRequired(false)
            .status(AttendeeStatus.ACCEPTED)
            .memberId(1)
            .build();

        Attendee attendee5 = Attendee.builder()
            .schedule(meetingSchedule2)
            .isRequired(true)
            .status(AttendeeStatus.ACCEPTED)
            .memberId(2)
            .build();

        Attendee attendee6 = Attendee.builder()
            .schedule(meetingSchedule2)
            .isRequired(true)
            .status(AttendeeStatus.PENDING)
            .memberId(3)
            .build();

        List<Attendee> meetingOneAttendeeList = List.of(attendee1, attendee2, attendee3);
        List<Attendee> meetingTwoAttendeeList = List.of(attendee4, attendee5, attendee6);
        meetingSchedule1.setAttendees(meetingOneAttendeeList);
        meetingSchedule2.setAttendees(meetingTwoAttendeeList);
        scheduleRepository.save(meetingSchedule1);
        scheduleRepository.save(meetingSchedule2);

        // 반복없는 회의 외 일정
        Schedule notRecurrenceSchedule1 = Schedule.builder()
            .organizerId(1)
            .name("반복없는 회의 외 일정1")
            .description("반복없는 회의 외 일정1")
            .type(ScheduleType.PERSONAL)
            .color(1)
            .isDeleted(false)
            .isPublic(true)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .build();

        Schedule notRecurrenceSchedule2 = Schedule.builder()
            .organizerId(1)
            .name("반복없는 회의 외 일정1")
            .description("반복없는 회의 외 일정1")
            .type(ScheduleType.PERSONAL)
            .color(1)
            .isDeleted(false)
            .isPublic(true)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 6, 1, 8, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 6, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .build();

        scheduleRepository.save(notRecurrenceSchedule1);
        scheduleRepository.save(notRecurrenceSchedule2);

        ScheduleListReadResponse scheduleListReadResponse1 = simpleScheduleService.getScheduleByPeriod(
            1,
            LocalDateTime.of(2024, 5, 1, 0, 0), LocalDateTime.of(2024, 5, 1, 23, 59));
        ScheduleListReadResponse scheduleListReadResponse2 = simpleScheduleService.getScheduleByPeriod(
            2, LocalDateTime.of(2024, 3, 1, 0, 0), LocalDateTime.of(2024, 6, 30, 23, 59)
        );

        for (IndividualSchedule scheduleResponse : scheduleListReadResponse1.getScheduleList()) {
            assertTrue(scheduleResponse.getStartDatetime().isBefore(
                LocalDateTime.of(2024, 5, 1, 23, 59)));
            assertTrue(scheduleResponse.getEndDatetime().isAfter(
                LocalDateTime.of(2024, 5, 1, 0, 0)));
        }

        for (IndividualSchedule scheduleResponse : scheduleListReadResponse2.getScheduleList()) {
            assertTrue(scheduleResponse.getStartDatetime().isBefore(
                LocalDateTime.of(2024, 6, 30, 23, 59)));
            assertTrue(scheduleResponse.getEndDatetime().isAfter(
                LocalDateTime.of(2024, 3, 1, 0, 0)));
        }
    }

    @DisplayName("반복횟수가 있는 회의 외 일정 조회 테스트")
    @Test
    void getRecurrenceScheduleExceptMeetingTest() {
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(4)
            .zoneId("Asia/Seoul")
            .build());

        // 반복하는 회의 외 일정
        Schedule recurrenceSchedule1 = Schedule.builder()
            .organizerId(4)
            .name("반복하는 회의 외 일정")
            .description("반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(2)
            .recurrence(Recurrence.builder()
                .count(3)
                .freq(RecurrenceFreqType.DAILY)
                .intv(7)
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule1);

        Schedule recurrenceSchedule2 = Schedule.builder()
            .organizerId(4)
            .name("반복하는 회의 외 일정")
            .description("반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(4)
            .recurrence(Recurrence.builder()
                .count(3)
                .freq(RecurrenceFreqType.MONTHLY)
                .intv(4)
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule2);

        Schedule recurrenceSchedule3 = Schedule.builder()
            .organizerId(4)
            .name("반복하는 회의 외 일정")
            .description("반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 1, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(4)
            .recurrence(Recurrence.builder()
                .count(10)
                .freq(RecurrenceFreqType.WEEKLY)
                .intv(4)
                .recurrenceDay(
                    EnumSet.of(RecurrenceDayType.MON, RecurrenceDayType.WED, RecurrenceDayType.FRI,
                        RecurrenceDayType.SAT,
                        RecurrenceDayType.SUN))
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule3);

        ScheduleListReadResponse response = simpleScheduleService.getScheduleByPeriod(4,
            LocalDateTime.of(2024, 5, 1, 0, 0), LocalDateTime.of(2024, 9, 30, 23, 59));

        for (IndividualSchedule scheduleResponse : response.getScheduleList()) {
            assertTrue(scheduleResponse.getStartDatetime().isBefore(
                LocalDateTime.of(2024, 9, 30, 23, 59)));
            assertTrue(scheduleResponse.getEndDatetime().isAfter(
                LocalDateTime.of(2024, 5, 1, 0, 0)));
        }
    }

    @DisplayName("기한이 있는 회의 외 반복 일정 조회")
    @Test
    void getRecurrenceAndExpiredDateScheduleExceptMeetingTest() {
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(5)
            .zoneId("Asia/Seoul")
            .build());

        // 반복하는 회의 외 일정
        Schedule recurrenceSchedule1 = Schedule.builder()
            .organizerId(5)
            .name("기한있는 일 반복하는 회의 외 일정")
            .description("기한 있는 반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(2)
            .recurrence(Recurrence.builder()
                .freq(RecurrenceFreqType.DAILY)
                .intv(7)
                .expiredDate(
                    AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 30, 0, 0),
                        ZoneId.of("Asia/Seoul")))
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule1);

        Schedule recurrenceSchedule2 = Schedule.builder()
            .organizerId(5)
            .name("기한있는 월 반복하는 회의 외 일정")
            .description("기한 있는 반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(2)
            .recurrence(Recurrence.builder()
                .freq(RecurrenceFreqType.MONTHLY)
                .intv(1)
                .expiredDate(
                    AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 10, 30, 0, 0),
                        ZoneId.of("Asia/Seoul")))
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule2);

        Schedule recurrenceSchedule3 = Schedule.builder()
            .organizerId(5)
            .name("기한있는 주 반복하는 회의 외 일정")
            .description("기한 있는 반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(2)
            .recurrence(Recurrence.builder()
                .freq(RecurrenceFreqType.WEEKLY)
                .intv(2)
                .recurrenceDay(
                    EnumSet.of(RecurrenceDayType.MON, RecurrenceDayType.WED, RecurrenceDayType.SAT,
                        RecurrenceDayType.SUN))
                .expiredDate(
                    AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2025, 10, 30, 0, 0),
                        ZoneId.of("Asia/Seoul")))
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule3);

        ScheduleListReadResponse response = simpleScheduleService.getScheduleByPeriod(5,
            LocalDateTime.of(2024, 5, 1, 0, 0),
            LocalDateTime.of(2024, 9, 30, 23, 59));

        for (IndividualSchedule scheduleResponse : response.getScheduleList()) {
            assertTrue(scheduleResponse.getStartDatetime().isBefore(
                LocalDateTime.of(2024, 9, 30, 23, 59)));
            assertTrue(scheduleResponse.getEndDatetime().isAfter(
                LocalDateTime.of(2024, 5, 1, 0, 0)));
        }
    }

    @DisplayName("기한 없는 회의 외 반복 일정 조회")
    @Test
    void getRecurrenceAndNotExpiredDateScheduleExceptMeetingTest() {
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(6)
            .zoneId("Asia/Seoul")
            .build());

        // 반복하는 회의 외 일정
        Schedule recurrenceSchedule1 = Schedule.builder()
            .organizerId(6)
            .name("기한없는 일 반복하는 회의 외 일정")
            .description("기한 없는 반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(2)
            .recurrence(Recurrence.builder()
                .freq(RecurrenceFreqType.DAILY)
                .intv(7)
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule1);

        Schedule recurrenceSchedule2 = Schedule.builder()
            .organizerId(6)
            .name("기한없는 월 반복하는 회의 외 일정")
            .description("기한 없는 반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(2)
            .recurrence(Recurrence.builder()
                .freq(RecurrenceFreqType.MONTHLY)
                .intv(1)
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule2);

        Schedule recurrenceSchedule3 = Schedule.builder()
            .organizerId(6)
            .name("기한없는 주 반복하는 회의 외 일정")
            .description("기한 없는 주 반복하는 회의 외 일정")
            .type(ScheduleType.PERSONAL)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 4, 30, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isDeleted(false)
            .isPublic(true)
            .color(2)
            .recurrence(Recurrence.builder()
                .freq(RecurrenceFreqType.WEEKLY)
                .intv(4)
                .recurrenceDay(EnumSet.of(RecurrenceDayType.MON, RecurrenceDayType.WED,
                    RecurrenceDayType.FRI, RecurrenceDayType.SUN))
                .build())
            .build();
        scheduleRepository.save(recurrenceSchedule3);

        ScheduleListReadResponse response = simpleScheduleService.getScheduleByPeriod(6,
            LocalDateTime.of(2024, 5, 1, 0, 0),
            LocalDateTime.of(2024, 9, 30, 23, 59));

        for (IndividualSchedule scheduleResponse : response.getScheduleList()) {
            assertTrue(scheduleResponse.getStartDatetime().isBefore(
                LocalDateTime.of(2024, 9, 30, 23, 59)));
            assertTrue(scheduleResponse.getEndDatetime().isAfter(
                LocalDateTime.of(2024, 5, 1, 0, 0)));
        }
    }

    @DisplayName("회의 참석 여부 선택하기")
    @Test
    void decideMeetingAttendanceTest() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(2)
            .zoneId("Europe/Paris")
            .build());

        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(3)
            .zoneId("Europe/London")
            .build());

        Schedule meetingSchedule = Schedule.builder()
            .organizerId(1)
            .name("회의")
            .description("회의 일정")
            .type(ScheduleType.MEETING)
            .color(1)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(true)
            .isDeleted(false)
            .build();

        // 참석자
        Attendee attendee1 = Attendee.builder()
            .schedule(meetingSchedule)
            .isRequired(true)
            .status(AttendeeStatus.ACCEPTED)
            .memberId(1)
            .build();

        Attendee attendee2 = Attendee.builder()
            .schedule(meetingSchedule)
            .isRequired(true)
            .status(AttendeeStatus.PENDING)
            .memberId(2)
            .build();

        Attendee attendee3 = Attendee.builder()
            .schedule(meetingSchedule)
            .isRequired(false)
            .status(AttendeeStatus.PENDING)
            .memberId(3)
            .build();

        List<Attendee> meetingAttendeeList = List.of(attendee1, attendee2, attendee3);
        meetingSchedule.setAttendees(meetingAttendeeList);
        Schedule savedMeetingSchedule = scheduleRepository.save(meetingSchedule);

        // 참석 여부 선택
        DecideAttendanceRequest decideAttendanceRequest2 = DecideAttendanceRequest.builder()
            .status(AttendeeStatus.ACCEPTED)
            .build();

        DecideAttendanceRequest decideAttendanceRequest3 = DecideAttendanceRequest.builder()
            .status(AttendeeStatus.DECLINED)
            .reason("시간이 안돼서")
            .startDatetime(LocalDateTime.of(2024, 5, 22, 11, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 22, 12, 0))
            .build();

        simpleScheduleService.decideAttendance(savedMeetingSchedule.getId(), 2,
            decideAttendanceRequest2);
        simpleScheduleService.decideAttendance(savedMeetingSchedule.getId(), 3,
            decideAttendanceRequest3);

        Attendee attendee2Result = attendeeRepository.findByScheduleIdAndMemberId(
            savedMeetingSchedule.getId(), 2).orElseThrow();
        Attendee attendee3Result = attendeeRepository.findByScheduleIdAndMemberId(
            savedMeetingSchedule.getId(), 3).orElseThrow();

        assertAll(
            () -> assertEquals(decideAttendanceRequest2.getStatus(), attendee2Result.getStatus()),
            () -> assertEquals(decideAttendanceRequest3.getStatus(), attendee3Result.getStatus()),
            () -> assertEquals(decideAttendanceRequest2.getReason(), attendee2Result.getReason()),
            () -> assertEquals(decideAttendanceRequest3.getReason(), attendee3Result.getReason()),
            () -> assertEquals(
                AlterTimeUtils.LocalDateTimeToInstant(decideAttendanceRequest3.getStartDatetime(),
                    ZoneId.of("Europe/London")),
                attendee3Result.getProposal().getStartDatetime()),
            () -> assertEquals(
                AlterTimeUtils.LocalDateTimeToInstant(decideAttendanceRequest3.getEndDatetime(),
                    ZoneId.of("Europe/London")),
                attendee3Result.getProposal().getEndDatetime())
        );
    }

    @DisplayName("회의 제안에 대해 시간 변경하기")
    @Test
    void changeScheduleTimeTest() {
        // memberTimezone 저장
        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(1)
            .zoneId("Asia/Seoul")
            .build());

        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(2)
            .zoneId("Europe/Paris")
            .build());

        memberTimezoneRepository.save(MemberTimezone.builder()
            .id(3)
            .zoneId("Europe/London")
            .build());

        Schedule meetingSchedule = Schedule.builder()
            .organizerId(1)
            .name("회의")
            .description("회의 일정")
            .type(ScheduleType.MEETING)
            .color(1)
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 9, 0),
                    ZoneId.of("Asia/Seoul")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 10, 0),
                    ZoneId.of("Asia/Seoul")))
            .isPublic(true)
            .isDeleted(false)
            .build();

        // 참석자
        Attendee attendee1 = Attendee.builder()
            .schedule(meetingSchedule)
            .isRequired(true)
            .status(AttendeeStatus.ACCEPTED)
            .memberId(1)
            .build();

        Attendee attendee2 = Attendee.builder()
            .schedule(meetingSchedule)
            .isRequired(true)
            .status(AttendeeStatus.PENDING)
            .memberId(2)
            .build();

        Attendee attendee3 = Attendee.builder()
            .schedule(meetingSchedule)
            .isRequired(false)
            .status(AttendeeStatus.PENDING)
            .memberId(3)
            .build();

        Proposal proposal1 = Proposal.builder()
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 10, 0),
                    ZoneId.of("Europe/Paris")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 11, 0),
                    ZoneId.of("Europe/Paris")))
            .build();

        Proposal proposal2 = Proposal.builder()
            .startDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 16, 0),
                    ZoneId.of("Europe/London")))
            .endDatetime(
                AlterTimeUtils.LocalDateTimeToInstant(LocalDateTime.of(2024, 5, 22, 17, 0),
                    ZoneId.of("Europe/London")))
            .build();
        proposalRepository.save(proposal1);
        proposalRepository.save(proposal2);

        attendee2.updateProposal(proposal1);
        attendee3.updateProposal(proposal2);

        List<Attendee> meetingAttendeeList = List.of(attendee1, attendee2, attendee3);
        meetingSchedule.setAttendees(meetingAttendeeList);
        Schedule savedMeetingSchedule = scheduleRepository.save(meetingSchedule);

        // 시간 변경
        ChangeScheduleTimeRequest changeScheduleTimeRequest = ChangeScheduleTimeRequest.builder()
            .startDatetime(LocalDateTime.of(2024, 5, 22, 10, 0))
            .endDatetime(LocalDateTime.of(2024, 5, 22, 11, 0)).build();

        simpleScheduleService.changeScheduleTime(1, savedMeetingSchedule.getId(),
            changeScheduleTimeRequest);

        List<Attendee> attendees = attendeeRepository.findBySchedule(savedMeetingSchedule);
        int proposalCount = 0;
        for (Attendee a : attendees) {
            if (a.getProposal() != null) {
                proposalCount++;
            }
        }
        assertEquals(0, proposalCount);
    }
}

