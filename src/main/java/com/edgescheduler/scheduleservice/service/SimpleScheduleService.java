package com.edgescheduler.scheduleservice.service;

import static com.edgescheduler.scheduleservice.util.AlterTimeUtils.LocalDateTimeToUTCLocalDateTime;

import com.edgescheduler.scheduleservice.client.UserServiceClient;
import com.edgescheduler.scheduleservice.domain.Attendee;
import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import com.edgescheduler.scheduleservice.domain.Proposal;
import com.edgescheduler.scheduleservice.domain.Recurrence;
import com.edgescheduler.scheduleservice.domain.RecurrenceDayType;
import com.edgescheduler.scheduleservice.domain.RecurrenceFreqType;
import com.edgescheduler.scheduleservice.domain.Schedule;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.request.DecideAttendanceRequest;
import com.edgescheduler.scheduleservice.dto.request.DeletedSchedule;
import com.edgescheduler.scheduleservice.dto.request.ResponseScheduleProposal;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest.ScheduleAttendee;
import com.edgescheduler.scheduleservice.dto.request.ScheduleDeleteRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleDeleteRequest.ScheduleDeleteRange;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest.RecurrenceDetails;
import com.edgescheduler.scheduleservice.dto.request.UpdatedSchedule;
import com.edgescheduler.scheduleservice.dto.response.*;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse.ScheduleDetailAttendee;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse.ScheduleProposal;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse.IndividualSchedule;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse.IndividualSchedule.MeetingScheduleDetail;
import com.edgescheduler.scheduleservice.exception.ErrorCode;
import com.edgescheduler.scheduleservice.message.AttendeeProposalMessage;
import com.edgescheduler.scheduleservice.message.AttendeeResponseMessage;
import com.edgescheduler.scheduleservice.message.KafkaEventMessage;
import com.edgescheduler.scheduleservice.message.MeetingCreateMessage;
import com.edgescheduler.scheduleservice.message.MeetingDeleteMessage;
import com.edgescheduler.scheduleservice.message.MeetingUpdateMessage;
import com.edgescheduler.scheduleservice.message.MeetingUpdateMessage.UpdatedField;
import com.edgescheduler.scheduleservice.message.Response;
import com.edgescheduler.scheduleservice.repository.AttendeeRepository;
import com.edgescheduler.scheduleservice.repository.MemberTimezoneRepository;
import com.edgescheduler.scheduleservice.repository.ProposalRepository;
import com.edgescheduler.scheduleservice.repository.RecurrenceRepository;
import com.edgescheduler.scheduleservice.repository.ScheduleRepository;
import com.edgescheduler.scheduleservice.util.AlterTimeUtils;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleScheduleService implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MemberTimezoneRepository memberTimezoneRepository;
    private final AttendeeRepository attendeeRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final ProposalRepository proposalRepository;
    private final KafkaProducer kafkaProducer;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public ScheduleCreateResponse createSchedule(ScheduleCreateRequest scheduleCreateRequest) {
        Integer organizerId = scheduleCreateRequest.getOrganizerId();
        String name = scheduleCreateRequest.getName();
        String description = scheduleCreateRequest.getDescription();
        ScheduleType type = scheduleCreateRequest.getType();
        Integer color = scheduleCreateRequest.getColor();
        LocalDateTime startDatetime = scheduleCreateRequest.getStartDatetime();
        LocalDateTime endDatetime = scheduleCreateRequest.getEndDatetime();
        Boolean isRecurrence = scheduleCreateRequest.getIsRecurrence();
        Boolean isPublic = scheduleCreateRequest.getIsPublic();

        // 시간 변환
        ZoneId zoneId = ZoneId.of(memberTimezoneRepository.findById(organizerId)
            .orElseThrow(ErrorCode.TIMEZONE_NOT_FOUND::build).getZoneId());
        Instant startDatetimeInstant = AlterTimeUtils.LocalDateTimeToInstant(startDatetime, zoneId);
        Instant endDatetimeInstant = AlterTimeUtils.LocalDateTimeToInstant(endDatetime, zoneId);

        Recurrence recurrence = null;
        // 반복되는 일정인 경우
        if (isRecurrence) {
            Instant expiredDate = null;
            // 기한이 존재하는 반복 일정의 경우
            if (scheduleCreateRequest.getRecurrence().getExpiredDate() != null) {
                expiredDate = AlterTimeUtils.LocalDateTimeToInstant(
                    scheduleCreateRequest.getRecurrence().getExpiredDate(), zoneId);
            }
            EnumSet<RecurrenceDayType> recurrenceDay = EnumSet.noneOf(RecurrenceDayType.class);
            // 반복 요일이 존재하는 일정의 경우
            if (!scheduleCreateRequest.getRecurrence().getRecurrenceDay().isEmpty()) {
                recurrenceDay.addAll(scheduleCreateRequest.getRecurrence().getRecurrenceDay());
            }
            recurrence = Recurrence.builder()
                .count(scheduleCreateRequest.getRecurrence().getCount())
                .freq(RecurrenceFreqType.valueOf(scheduleCreateRequest.getRecurrence().getFreq()))
                .intv(scheduleCreateRequest.getRecurrence().getIntv()).expiredDate(expiredDate)
                .recurrenceDay(recurrenceDay).build();

        }
        Schedule schedule = Schedule.builder().organizerId(organizerId).name(name)
            .description(description).type(type).color(color).startDatetime(startDatetimeInstant)
            .endDatetime(endDatetimeInstant).isPublic(isPublic).isDeleted(false)
            .recurrence(recurrence).build();
        Schedule saveSchedule = scheduleRepository.save(schedule);
        // 회의 일정의 경우
        if (type.equals(ScheduleType.MEETING)) {
            List<ScheduleAttendee> attendeeList = scheduleCreateRequest.getAttendeeList();
            if (!attendeeList.isEmpty()) {
                attendeeList.stream().map(
                    attendee -> Attendee.builder().isRequired(attendee.getIsRequired())
                        .memberId(attendee.getMemberId()).status(AttendeeStatus.PENDING)
                        .schedule(saveSchedule).build()).forEach(attendeeRepository::save);
            }
            List<Integer> attendeeIds = new ArrayList<>();
            for (ScheduleAttendee attendee : attendeeList) {
                attendeeIds.add(attendee.getMemberId());
            }
            UserInfoResponse response = userServiceClient.getUserName(
                saveSchedule.getOrganizerId());
            KafkaEventMessage message = MeetingCreateMessage.builder()
                .occurredAt(LocalDateTimeToUTCLocalDateTime(LocalDateTime.now(), zoneId))
                .scheduleId(saveSchedule.getId())
                .organizerId(saveSchedule.getOrganizerId())
                .organizerName(response != null ? response.getName() : null)
                .startTime(
                    LocalDateTime.ofInstant(saveSchedule.getStartDatetime(), ZoneId.of("UTC")))
                .endTime(LocalDateTime.ofInstant(saveSchedule.getEndDatetime(), ZoneId.of("UTC")))
                .attendeeIds(attendeeIds)
                .build();
            kafkaProducer.send("meeting-created", message);
        }
        return ScheduleCreateResponse.builder().scheduleId(saveSchedule.getId()).build();
    }

    @Override
    public ScheduleDetailReadResponse getSchedule(Integer memberId, Long id) {
        // 해당 일정 조회
        Schedule schedule = scheduleRepository.findById(id)
            .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::build);
        // 조회하는 사람 기준의 시간대
        ZoneId zoneId = ZoneId.of(memberTimezoneRepository.findById(memberId)
            .orElseThrow(ErrorCode.TIMEZONE_NOT_FOUND::build).getZoneId());

        List<Attendee> attendees = attendeeRepository.findBySchedule(schedule);
        List<ScheduleDetailAttendee> attendeeList = new ArrayList<>();
        // 일정 공유하는 사람이 있는 경우
        if (!attendees.isEmpty()) {
            for (Attendee attendee : attendees) {
                ScheduleProposal scheduleProposal = null;
                // 시간 제안한 경우가 있는 경우
                if (attendee.getProposal() != null) {
                    scheduleProposal = ScheduleProposal.builder()
                        .proposalId(attendee.getProposal().getId()).startDatetime(
                            AlterTimeUtils.instantToLocalDateTime(
                                attendee.getProposal().getStartDatetime(), zoneId)).endDatetime(
                            AlterTimeUtils.instantToLocalDateTime(
                                attendee.getProposal().getEndDatetime(), zoneId)).build();
                }
                UserInfoResponse response = userServiceClient.getUserName(attendee.getMemberId());
                ScheduleDetailAttendee attendeeDetail = ScheduleDetailAttendee.builder()
                    .memberId(attendee.getMemberId())
                    .memberName(response != null ? response.getName() : null)
                    .isRequired(attendee.getIsRequired())
                    .status(attendee.getStatus()).reason(attendee.getReason())
                    .proposal(scheduleProposal).build();

                attendeeList.add(attendeeDetail);
            }
        }

        ScheduleDetailReadResponse.RecurrenceDetails recurrenceDetails = null;
        if (schedule.getRecurrence() != null) {
            // 반복 요일이 존재하는 일정의 경우
            EnumSet<RecurrenceDayType> recurrenceDays = EnumSet.noneOf(RecurrenceDayType.class);
            if (schedule.getRecurrence().getRecurrenceDay() != null) {
                schedule.getRecurrence().getRecurrenceDay().forEach(
                    day -> recurrenceDays.add(RecurrenceDayType.valueOf(String.valueOf(day))));
            }
            LocalDateTime expiredDatetime = null;
            if (schedule.getRecurrence().getExpiredDate() != null) {
                expiredDatetime = AlterTimeUtils.instantToLocalDateTime(
                    schedule.getRecurrence().getExpiredDate(), zoneId);
            }
            recurrenceDetails = ScheduleDetailReadResponse.RecurrenceDetails.builder()
                .count(schedule.getRecurrence().getCount() != null ? schedule.getRecurrence()
                    .getCount() : null).intv(schedule.getRecurrence().getIntv())
                .expiredDate(expiredDatetime)
                .recurrenceDay(recurrenceDays)
                .freq(String.valueOf(schedule.getRecurrence().getFreq())).build();
        }

        return ScheduleDetailReadResponse.builder().scheduleId(schedule.getId())
            .organizerId(schedule.getOrganizerId()).name(schedule.getName())
            .description(schedule.getDescription()).type(schedule.getType())
            .color(schedule.getColor()).startDatetime(
                AlterTimeUtils.instantToLocalDateTime(schedule.getStartDatetime(), zoneId))
            .endDatetime(AlterTimeUtils.instantToLocalDateTime(schedule.getEndDatetime(), zoneId))
            .isPublic(schedule.getIsPublic()).attendeeList(attendeeList)
            .recurrenceDetails(recurrenceDetails).build();
    }

    @Override
    public SimpleScheduleInfoResponse getSimpleSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(ErrorCode.SCHEDULE_NOT_FOUND::build);
        return SimpleScheduleInfoResponse.builder()
                .scheduleId(schedule.getId())
                .name(schedule.getName())
                .organizerId(schedule.getOrganizerId())
                .organizerName(userServiceClient.getUserName(schedule.getOrganizerId()).getName())
                .startDatetime(AlterTimeUtils.instantToLocalDateTime(schedule.getStartDatetime(),
                        ZoneId.of("UTC")))
                .endDatetime(AlterTimeUtils.instantToLocalDateTime(schedule.getEndDatetime(),
                        ZoneId.of("UTC")))
                .build();
    }

    @Override
    public ScheduleListReadResponse getScheduleByPeriod(Integer memberId, LocalDateTime start,
        LocalDateTime end) {
        ZoneId zoneId = ZoneId.of(
            memberTimezoneRepository.findById(memberId).orElseThrow().getZoneId());

        Instant startInstant = AlterTimeUtils.LocalDateTimeToInstant(start, zoneId);
        Instant endInstant = AlterTimeUtils.LocalDateTimeToInstant(end, zoneId);
        // 회의가 아닌 내 일정 리스트
        List<Schedule> schedulesExceptMeetingList = scheduleRepository.findSchedulesExceptMeetingByOrganizerId(
            memberId);
        // 내가 참여 중인 attendee 리스트
        List<Attendee> attendeeList = attendeeRepository.findByMemberId(memberId);
        // 최종적으로 조회할 결과값 리스트
        List<IndividualSchedule> scheduleResultList = new ArrayList<>();
        // 삭제된 회의 외 일정 리스트
        List<DeletedSchedule> deleteScheduleList = new ArrayList<>();
        // 수정된 회의 외 일정 리스트
        List<UpdatedSchedule> updatedScheduleList = new ArrayList<>();
        // 회의 외 일정
        for (Schedule s : schedulesExceptMeetingList) {
            if (s.getRecurrence() == null) {
                // 반복일정이 아니면서 기한에서 벗어나는 경우
                if (s.getEndDatetime().isBefore(startInstant)
                    || s.getStartDatetime().isAfter(endInstant)) {
                    continue;
                }
                // 반복일정 중 선택 삭제된 일정 경우
                if (s.getIsDeleted()) {
                    DeletedSchedule deletedSchedule = DeletedSchedule.builder()
                        .parentScheduleId(s.getSchedule().getId())
                        .deleteDateInstant(s.getStartDatetime())
                        .build();
                    deleteScheduleList.add(deletedSchedule);
                    continue;
                }
                // 반복일정 중 선택 수정된 일정 경우
                if (s.getSchedule() != null) {
                    UpdatedSchedule updatedSchedule = UpdatedSchedule.builder()
                        .parentScheduleId(s.getSchedule().getId())
                        .updateDateInstant(s.getStartDatetime())
                        .build();
                    updatedScheduleList.add(updatedSchedule);
                }
                // 반복일정 아니면서 기한에 있는 경우
                IndividualSchedule result = IndividualSchedule.builder()
                    .scheduleId(s.getId())
                    .organizerId(s.getOrganizerId())
                    .name(s.getName()).type(s.getType())
                    .color(s.getColor())
                    .startDatetime(
                        AlterTimeUtils.instantToLocalDateTime(s.getStartDatetime(), zoneId))
                    .endDatetime(
                        AlterTimeUtils.instantToLocalDateTime(s.getEndDatetime(), zoneId))
                    .isPublic(s.getIsPublic()).build();
                scheduleResultList.add(result);
                continue;
            }
            // 반복일정 & 시작 기한이 조회 기간보다 늦는 경우
            if (s.getStartDatetime().isAfter(endInstant)) {
                continue;
            }
            // 반복일정 & 반복 기한이 다 된 경우
            if (s.getRecurrence().getExpiredDate() != null
                && s.getRecurrence().getExpiredDate().isBefore(startInstant)) {
                continue;
            }
            RecurrenceFreqType freq = s.getRecurrence().getFreq();
            Integer intv = s.getRecurrence().getIntv();

            // 반복 횟수가 있는 반복 일정
            if (s.getRecurrence().getCount() != null) {
                int count = s.getRecurrence().getCount();
                LocalDateTime endLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                    s.getEndDatetime(), zoneId);
                LocalDateTime startLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                    s.getStartDatetime(), zoneId);
                switch (freq) {
                    // 일일 반복
                    case DAILY:
                        endLocalDatetime = endLocalDatetime.plusDays((long) intv * count);
                        // 마지막 반복이 조회기간 전에 끝나는 경우
                        if (AlterTimeUtils.LocalDateTimeToInstant(endLocalDatetime, zoneId)
                            .isBefore(startInstant)) {
                            continue;
                        }
                        endLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                            s.getEndDatetime(), zoneId);
                        for (int i = 0; i < count;
                            i++, startLocalDatetime = startLocalDatetime.plusDays(
                                intv), endLocalDatetime = endLocalDatetime.plusDays(intv)) {
                            // 반복이 조회기간을 벗어나면 넘어가
                            boolean isOutOfPeriod = isOutOfPeriod(startLocalDatetime,
                                startInstant, endLocalDatetime, endInstant, zoneId);
                            if (isOutOfPeriod) {
                                continue;
                            }
                            // 삭제된 스케줄인 경우 넘어가기
                            boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isDeletedSchedule) {
                                continue;
                            }
                            // 수정된 스케줄인 경우 넘어가기
                            boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList,
                                s,
                                zoneId, startLocalDatetime);
                            if (isUpdatedSchedule) {
                                continue;
                            }
                            IndividualSchedule result = IndividualSchedule.builder()
                                .scheduleId(s.getId())
                                .organizerId(s.getOrganizerId())
                                .name(s.getName())
                                .type(s.getType())
                                .color(s.getColor())
                                .startDatetime(startLocalDatetime)
                                .endDatetime(endLocalDatetime)
                                .isPublic(s.getIsPublic()).build();
                            scheduleResultList.add(result);
                        }
                        break;
                    case MONTHLY:
                        endLocalDatetime = endLocalDatetime.plusMonths((long) intv * count);
                        // 마지막 반복이 조회기간 전에 끝나는 경우
                        if (AlterTimeUtils.LocalDateTimeToInstant(endLocalDatetime, zoneId)
                            .isBefore(startInstant)) {
                            continue;
                        }
                        // 반복이 조회기간에 존재하면 추가
                        endLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                            s.getEndDatetime(), zoneId);
                        for (int i = 0; i < count;
                            i++, startLocalDatetime = startLocalDatetime.plusMonths(
                                intv), endLocalDatetime = endLocalDatetime.plusMonths(intv)) {
                            // 반복이 조회기간을 벗어나면 넘어가
                            boolean isOutOfPeriod = isOutOfPeriod(startLocalDatetime,
                                startInstant, endLocalDatetime, endInstant, zoneId);
                            if (isOutOfPeriod) {
                                continue;
                            }
                            // 삭제된 일정인 경우 넘어가
                            boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isDeletedSchedule) {
                                continue;
                            }
                            // 수정된 일정인 경우 넘어가
                            boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList,
                                s,
                                zoneId, startLocalDatetime);
                            if (isUpdatedSchedule) {
                                continue;
                            }

                            IndividualSchedule result = IndividualSchedule.builder()
                                .scheduleId(s.getId())
                                .organizerId(s.getOrganizerId())
                                .name(s.getName())
                                .type(s.getType())
                                .color(s.getColor())
                                .startDatetime(startLocalDatetime)
                                .endDatetime(endLocalDatetime)
                                .isPublic(s.getIsPublic()).build();
                            scheduleResultList.add(result);
                        }
                        break;
                    default:
                        EnumSet<RecurrenceDayType> recurrenceDay = s.getRecurrence()
                            .getRecurrenceDay();
                        // 시작 요일
                        DayOfWeek startDay = AlterTimeUtils.instantToLocalDateTime(
                            s.getStartDatetime(), zoneId).getDayOfWeek();
                        // 반복 요일 정렬
                        List<Integer> dayList = sortDayList(recurrenceDay, startDay);
                        // 반복의 마지막 날
                        endLocalDatetime = endLocalDatetime.plusDays(
                            dayList.get(dayList.size() - 1)).plusWeeks((long) intv * count);
                        // 반복 마지막이 조회기간 전에 끝나는 경우
                        if (endLocalDatetime.isBefore(startLocalDatetime)) {
                            continue;
                        }
                        endLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                            s.getEndDatetime(), zoneId);
                        weekly:
                        for (int i = 0; i < count;
                            i++, startLocalDatetime = startLocalDatetime.plusWeeks(
                                intv), endLocalDatetime = endLocalDatetime.plusWeeks(intv)) {
                            for (Integer day : dayList) {
                                LocalDateTime weekStartLocalDatetime = startLocalDatetime.plusDays(
                                    day);
                                LocalDateTime weekEndLocalDatetime = endLocalDatetime.plusDays(day);
                                // 반복 시작이 조회 마지막 기간 이후인 경우 반복 종료
                                if (AlterTimeUtils.LocalDateTimeToInstant(weekStartLocalDatetime,
                                    zoneId).isAfter(endInstant)) {
                                    break weekly;
                                }
                                // 반복이 조회 기간을 벗어나는 경우 넘어가
                                boolean isOutOfPeriod = isOutOfPeriod(weekStartLocalDatetime,
                                    startInstant, weekEndLocalDatetime, endInstant, zoneId);
                                if (isOutOfPeriod) {
                                    continue;
                                }
                                // 삭제된 일정인 경우 넘어가
                                boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                    zoneId, weekStartLocalDatetime);
                                if (isDeletedSchedule) {
                                    continue;
                                }
                                // 수정된 일정인 경우 넘어가
                                boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList,
                                    s,
                                    zoneId, weekStartLocalDatetime);
                                if (isUpdatedSchedule) {
                                    continue;
                                }

                                IndividualSchedule result = IndividualSchedule.builder()
                                    .scheduleId(s.getId())
                                    .organizerId(s.getOrganizerId())
                                    .name(s.getName())
                                    .type(s.getType())
                                    .color(s.getColor())
                                    .startDatetime(weekStartLocalDatetime)
                                    .endDatetime(weekEndLocalDatetime)
                                    .isPublic(s.getIsPublic()).build();
                                scheduleResultList.add(result);
                            }
                        }
                        break;
                }
                continue;
            }
            // 반복일정 & 반복 기한이 있는 반복
            if (s.getRecurrence().getExpiredDate() != null) {
                LocalDateTime startLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                    s.getStartDatetime(), zoneId);
                LocalDateTime endLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                    s.getEndDatetime(), zoneId);
                switch (freq) {
                    case DAILY:
                        // 반복 시작일이 반복 기한 전이면서, 조회 마지막 기간 이전인 경우 계속 반복
                        while (AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(s.getRecurrence().getExpiredDate())
                            && AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(endInstant)) {
                            // 반복 종료일이 조회 시작기간보다 이전인 경우 그냥 넘어가
                            if (AlterTimeUtils.LocalDateTimeToInstant(
                                endLocalDatetime, zoneId).isBefore(startInstant)) {
                                startLocalDatetime = startLocalDatetime.plusDays(intv);
                                endLocalDatetime = endLocalDatetime.plusDays(intv);
                                continue;
                            }

                            boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isDeletedSchedule) {
                                startLocalDatetime = startLocalDatetime.plusDays(intv);
                                endLocalDatetime = endLocalDatetime.plusDays(intv);
                                continue;
                            }

                            boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isUpdatedSchedule) {
                                startLocalDatetime = startLocalDatetime.plusDays(intv);
                                endLocalDatetime = endLocalDatetime.plusDays(intv);
                                continue;
                            }

                            IndividualSchedule individualSchedule = IndividualSchedule.builder()
                                .scheduleId(s.getId())
                                .name(s.getName())
                                .organizerId(s.getOrganizerId())
                                .type(s.getType())
                                .color(s.getColor())
                                .startDatetime(startLocalDatetime)
                                .endDatetime(endLocalDatetime)
                                .isPublic(s.getIsPublic())
                                .build();
                            scheduleResultList.add(individualSchedule);
                            startLocalDatetime = startLocalDatetime.plusDays(intv);
                            endLocalDatetime = endLocalDatetime.plusDays(intv);
                        }
                        break;
                    case MONTHLY:
                        // 반복 시작일이 반복 기한 전이면서, 조회 마지막 기간 이전인 경우 계속 반복
                        while (AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(s.getRecurrence().getExpiredDate())
                            && AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(endInstant)) {
                            // 반복 종료일이 조회 시작기간보다 이전인 경우 그냥 넘어가
                            if (AlterTimeUtils.LocalDateTimeToInstant(
                                endLocalDatetime, zoneId).isBefore(startInstant)) {
                                startLocalDatetime = startLocalDatetime.plusMonths(intv);
                                endLocalDatetime = endLocalDatetime.plusMonths(intv);
                                continue;
                            }

                            boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isDeletedSchedule) {
                                startLocalDatetime = startLocalDatetime.plusMonths(intv);
                                endLocalDatetime = endLocalDatetime.plusMonths(intv);
                                continue;
                            }

                            boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isUpdatedSchedule) {
                                startLocalDatetime = startLocalDatetime.plusMonths(intv);
                                endLocalDatetime = endLocalDatetime.plusMonths(intv);
                                continue;
                            }

                            IndividualSchedule individualSchedule = IndividualSchedule.builder()
                                .scheduleId(s.getId())
                                .name(s.getName())
                                .organizerId(s.getOrganizerId())
                                .type(s.getType())
                                .color(s.getColor())
                                .startDatetime(startLocalDatetime)
                                .endDatetime(endLocalDatetime)
                                .isPublic(s.getIsPublic())
                                .build();
                            scheduleResultList.add(individualSchedule);
                            startLocalDatetime = startLocalDatetime.plusMonths(intv);
                            endLocalDatetime = endLocalDatetime.plusMonths(intv);
                        }
                        break;
                    default:
                        EnumSet<RecurrenceDayType> recurrenceDay = s.getRecurrence()
                            .getRecurrenceDay();
                        // 시작 요일
                        DayOfWeek startDay = AlterTimeUtils.instantToLocalDateTime(
                            s.getStartDatetime(), zoneId).getDayOfWeek();
                        // 반복 요일 정렬
                        List<Integer> dayList = sortDayList(recurrenceDay, startDay);
                        int idx = 0;
                        weekly:
                        // 반복시작일이 만료기한 이전이면서 조회 마지막 기간 이전인 경우 계속 반복
                        while (AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(s.getRecurrence().getExpiredDate())
                            && AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(endInstant)) {
                            startLocalDatetime = startLocalDatetime.plusWeeks((long) intv * idx);
                            endLocalDatetime = endLocalDatetime.plusWeeks((long) intv * idx);
                            idx++;
                            for (Integer day : dayList) {
                                LocalDateTime weekStartLocalDatetime = startLocalDatetime.plusDays(
                                    day);
                                LocalDateTime weekEndLocalDatetime = endLocalDatetime.plusDays(day);
                                // 반복 시작이 조회 마지막 기간 이후인 경우 반복 종료
                                if (AlterTimeUtils.LocalDateTimeToInstant(weekStartLocalDatetime,
                                    zoneId).isAfter(endInstant)) {
                                    break weekly;
                                }
                                // 반복이 조회 기간을 벗어나는 경우 넘어가
                                boolean isOutOfPeriod = isOutOfPeriod(weekStartLocalDatetime,
                                    startInstant, weekEndLocalDatetime, endInstant, zoneId);
                                if (isOutOfPeriod) {
                                    continue;
                                }
                                // 삭제된 일정인 경우 넘어가
                                boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                    zoneId, weekStartLocalDatetime);
                                if (isDeletedSchedule) {
                                    continue;
                                }
                                // 수정된 일정인 경우 넘어가
                                boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList,
                                    s,
                                    zoneId, weekStartLocalDatetime);
                                if (isUpdatedSchedule) {
                                    continue;
                                }

                                IndividualSchedule result = IndividualSchedule.builder()
                                    .scheduleId(s.getId())
                                    .organizerId(s.getOrganizerId())
                                    .name(s.getName())
                                    .type(s.getType())
                                    .color(s.getColor())
                                    .startDatetime(weekStartLocalDatetime)
                                    .endDatetime(weekEndLocalDatetime)
                                    .isPublic(s.getIsPublic()).build();
                                scheduleResultList.add(result);
                            }
                        }
                        break;
                }
            }
            // 반복일정 & 기한이 없는 반복
            if (s.getRecurrence().getExpiredDate() == null) {
                LocalDateTime startLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                    s.getStartDatetime(), zoneId);
                LocalDateTime endLocalDatetime = AlterTimeUtils.instantToLocalDateTime(
                    s.getEndDatetime(), zoneId);
                switch (freq) {
                    case DAILY:
                        // 반복 시작일이 조회 마지막 기간 이전인 경우 계속 반복
                        while (AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(endInstant)) {
                            // 반복 종료일이 조회 시작기간보다 이전인 경우 그냥 넘어가
                            if (AlterTimeUtils.LocalDateTimeToInstant(endLocalDatetime, zoneId)
                                .isBefore(startInstant)
                            ) {
                                startLocalDatetime = startLocalDatetime.plusDays(intv);
                                endLocalDatetime = endLocalDatetime.plusDays(intv);
                                continue;
                            }

                            boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isDeletedSchedule) {
                                startLocalDatetime = startLocalDatetime.plusDays(intv);
                                endLocalDatetime = endLocalDatetime.plusDays(intv);
                                continue;
                            }

                            boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isUpdatedSchedule) {
                                startLocalDatetime = startLocalDatetime.plusDays(intv);
                                endLocalDatetime = endLocalDatetime.plusDays(intv);
                                continue;
                            }

                            IndividualSchedule individualSchedule = IndividualSchedule.builder()
                                .name(s.getName())
                                .organizerId(memberId)
                                .scheduleId(s.getId())
                                .type(s.getType())
                                .color(s.getColor())
                                .startDatetime(startLocalDatetime)
                                .endDatetime(endLocalDatetime)
                                .isPublic(s.getIsPublic())
                                .build();
                            scheduleResultList.add(individualSchedule);

                            startLocalDatetime = startLocalDatetime.plusDays(intv);
                            endLocalDatetime = endLocalDatetime.plusDays(intv);
                        }
                        break;
                    case MONTHLY:
                        // 반복 종료일이 조회 마지막 기간 이전인 경우 계속 반복
                        while (AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(endInstant)) {
                            // 반복 종료일이 조회 시작기간보다 이전인 경우 그냥 넘어가
                            if (AlterTimeUtils.LocalDateTimeToInstant(endLocalDatetime, zoneId)
                                .isBefore(startInstant)
                            ) {
                                startLocalDatetime = startLocalDatetime.plusMonths(intv);
                                endLocalDatetime = endLocalDatetime.plusMonths(intv);
                                continue;
                            }
                            boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isDeletedSchedule) {
                                startLocalDatetime = startLocalDatetime.plusMonths(intv);
                                endLocalDatetime = endLocalDatetime.plusMonths(intv);
                                continue;
                            }

                            boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList, s,
                                zoneId, startLocalDatetime);
                            if (isUpdatedSchedule) {
                                startLocalDatetime = startLocalDatetime.plusMonths(intv);
                                endLocalDatetime = endLocalDatetime.plusMonths(intv);
                                continue;
                            }
                            IndividualSchedule individualSchedule = IndividualSchedule.builder()
                                .name(s.getName())
                                .organizerId(memberId)
                                .scheduleId(s.getId())
                                .type(s.getType())
                                .color(s.getColor())
                                .startDatetime(startLocalDatetime)
                                .endDatetime(endLocalDatetime)
                                .isPublic(s.getIsPublic())
                                .build();
                            scheduleResultList.add(individualSchedule);

                            startLocalDatetime = startLocalDatetime.plusMonths(intv);
                            endLocalDatetime = endLocalDatetime.plusMonths(intv);
                        }
                        break;
                    default:
                        EnumSet<RecurrenceDayType> recurrenceDay = s.getRecurrence()
                            .getRecurrenceDay();
                        // 시작 요일
                        DayOfWeek startDay = AlterTimeUtils.instantToLocalDateTime(
                            s.getStartDatetime(), zoneId).getDayOfWeek();
                        // 반복 요일 정렬
                        List<Integer> dayList = sortDayList(recurrenceDay, startDay);
                        int idx = 0;
                        weekly:
                        // 반복시작일이 조회 마지막 기간 이전인 경우 계속 반복
                        while (AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
                            .isBefore(endInstant)) {
                            if (idx > 0) {
                                startLocalDatetime = startLocalDatetime.plusWeeks((long) intv);
                                endLocalDatetime = endLocalDatetime.plusWeeks((long) intv);
                            }
                            idx++;
                            for (Integer day : dayList) {
                                LocalDateTime weekStartLocalDatetime = startLocalDatetime.plusDays(
                                    day);
                                LocalDateTime weekEndLocalDatetime = endLocalDatetime.plusDays(day);
                                // 반복 시작이 조회 마지막 기간 이후인 경우 반복 종료
                                if (AlterTimeUtils.LocalDateTimeToInstant(weekStartLocalDatetime,
                                    zoneId).isAfter(endInstant)) {
                                    break weekly;
                                }
                                // 반복이 조회 기간을 벗어나는 경우 넘어가
                                boolean isOutOfPeriod = isOutOfPeriod(weekStartLocalDatetime,
                                    startInstant, weekEndLocalDatetime, endInstant, zoneId);
                                if (isOutOfPeriod) {
                                    continue;
                                }
                                // 삭제된 일정인 경우 넘어가
                                boolean isDeletedSchedule = isDeletedSchedule(deleteScheduleList, s,
                                    zoneId, weekStartLocalDatetime);
                                if (isDeletedSchedule) {
                                    continue;
                                }
                                // 수정된 일정인 경우 넘어가
                                boolean isUpdatedSchedule = isUpdatedSchedule(updatedScheduleList,
                                    s,
                                    zoneId, weekStartLocalDatetime);
                                if (isUpdatedSchedule) {
                                    continue;
                                }

                                IndividualSchedule result = IndividualSchedule.builder()
                                    .scheduleId(s.getId())
                                    .organizerId(s.getOrganizerId())
                                    .name(s.getName())
                                    .type(s.getType())
                                    .color(s.getColor())
                                    .startDatetime(weekStartLocalDatetime)
                                    .endDatetime(weekEndLocalDatetime)
                                    .isPublic(s.getIsPublic()).build();
                                scheduleResultList.add(result);
                            }
                        }
                        break;
                }
            }
        }
        // 회의 일정
        for (Attendee a : attendeeList) {
            Schedule meetingWithMe = scheduleRepository.findById(
                a.getSchedule().getId()).orElseThrow();
            if (meetingWithMe.getStartDatetime().isAfter(endInstant)) {
                continue;
            }
            if (meetingWithMe.getEndDatetime().isBefore(startInstant)) {
                continue;
            }
            IndividualSchedule individualSchedule = IndividualSchedule.builder()
                .scheduleId(meetingWithMe.getId())
                .organizerId(meetingWithMe.getOrganizerId())
                .name(meetingWithMe.getName())
                .type(meetingWithMe.getType())
                .color(meetingWithMe.getColor())
                .startDatetime(
                    AlterTimeUtils.instantToLocalDateTime(meetingWithMe.getStartDatetime(), zoneId))
                .endDatetime(
                    AlterTimeUtils.instantToLocalDateTime(meetingWithMe.getEndDatetime(), zoneId))
                .isPublic(meetingWithMe.getIsPublic())
                .meetingDetail(MeetingScheduleDetail.builder()
                    .isRequired(a.getIsRequired())
                    .status(String.valueOf(a.getStatus()))
                    .reason(a.getReason())
                    .build())
                .build();

            scheduleResultList.add(individualSchedule);
        }
        // 조회 기간 내 일정이 없는 경우
        if (scheduleResultList.isEmpty()) {
            throw ErrorCode.SCHEDULE_NOT_FOUND.build();
        }

        return ScheduleListReadResponse.builder()
            .scheduleList(scheduleResultList)
            .build();
    }

    @Override
    @Transactional
    public ScheduleUpdateResponse updateSchedule(Integer memberId, Long scheduleId,
        ScheduleUpdateRequest scheduleRequest) {
        // requestInput
        String name = scheduleRequest.getName();
        String description = scheduleRequest.getDescription();
        ScheduleType type = scheduleRequest.getType();
        Integer color = scheduleRequest.getColor();
        LocalDateTime startDatetime = scheduleRequest.getStartDatetime();
        LocalDateTime endDatetime = scheduleRequest.getEndDatetime();
        Boolean isPublic = scheduleRequest.getIsPublic();
        Boolean isRecurrence = scheduleRequest.getIsRecurrence();
        Boolean isOneOff = scheduleRequest.getIsOneOff();
        RecurrenceDetails recurrence = scheduleRequest.getRecurrence();
        List<ScheduleUpdateRequest.ScheduleAttendee> attendeeList = scheduleRequest.getAttendeeList();
        Boolean nameIsChanged = scheduleRequest.getNameIsChanged();
        Boolean descriptionIsChanged = scheduleRequest.getDescriptionIsChanged();
        Boolean timeIsChanged = scheduleRequest.getTimeIsChanged();

        ZoneId zoneId = ZoneId.of(
            memberTimezoneRepository.findById(memberId).orElseThrow().getZoneId());
        Instant startInstant = AlterTimeUtils.LocalDateTimeToInstant(startDatetime, zoneId);
        Instant endInstant = AlterTimeUtils.LocalDateTimeToInstant(endDatetime, zoneId);

        Schedule savedSchedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("null"));
        Integer organizerId = savedSchedule.getOrganizerId();
        // 주최자가 아닌 경우 오류
        validateOrganizer(savedSchedule, memberId);
        // 회의 외의 일정인 경우
        if (!String.valueOf(savedSchedule.getType()).equals("MEETING")) {
            // 반복 일정 & 해당 이벤트만 수정
            if (isRecurrence && isOneOff) {
                // 해당 날짜에 이미 수정된 이력이 있는 일정인 경우 삭제
                List<Schedule> alreadyModifiedScheduleList = scheduleRepository.findModifiedScheduleByParentSchedule(
                    savedSchedule);
                if (alreadyModifiedScheduleList != null && !alreadyModifiedScheduleList.isEmpty()) {
                    for (Schedule ms : alreadyModifiedScheduleList) {
                        if (AlterTimeUtils.instantToLocalDateTime(ms.getStartDatetime(), zoneId)
                            .toLocalDate().equals(startDatetime.toLocalDate())) {
                            scheduleRepository.delete(ms);
                            break;
                        }
                    }
                }
                Schedule modifiedSchedule = Schedule.builder().organizerId(organizerId)
                    .name(name)
                    .description(description).type(type).startDatetime(startInstant)
                    .endDatetime(endInstant).isPublic(isPublic).color(color).isDeleted(false)
                    .schedule(savedSchedule).build();
                Schedule result = scheduleRepository.save(modifiedSchedule);
                return ScheduleUpdateResponse.builder().scheduleId(result.getId()).build();
            }
            // 반복 일정 & 이후 모든 이벤트 수정
            if (isRecurrence && !isOneOff) {
                // 기본 반복 기한 오늘 날짜로 수정하기
                savedSchedule.getRecurrence().terminateRecurrenceByDate(LocalDateTime.now());
                // 새로운 반복 일정 추가하기
                String freq = recurrence.getFreq();
                Integer intv = recurrence.getIntv();
                LocalDateTime expiredDate = recurrence.getExpiredDate();
                Instant expiredInstant = null;
                if (expiredDate != null) {
                    expiredInstant = AlterTimeUtils.LocalDateTimeToInstant(expiredDate, zoneId);
                }
                Integer count = recurrence.getCount();
                EnumSet<RecurrenceDayType> recurrenceDay = EnumSet.noneOf(RecurrenceDayType.class);
                if (recurrence.getRecurrenceDay() != null) {
                    recurrence.getRecurrenceDay().forEach(
                        day -> recurrenceDay.add(RecurrenceDayType.valueOf(String.valueOf(day))));
                }
                Recurrence newRecurrence = Recurrence.builder()
                    .freq(freq != null ? RecurrenceFreqType.valueOf(freq) : null).intv(intv)
                    .expiredDate(expiredInstant).count(count).recurrenceDay(recurrenceDay)
                    .build();

                // 일정 추가하기
                Schedule modifiedSchedule = Schedule.builder().organizerId(organizerId)
                    .name(name)
                    .description(description).type(type).startDatetime(startInstant)
                    .endDatetime(endInstant).isPublic(isPublic).color(color)
                    .schedule(savedSchedule)
                    .recurrence(newRecurrence).isDeleted(false).build();
                Schedule result = scheduleRepository.save(modifiedSchedule);
                return ScheduleUpdateResponse.builder().scheduleId(result.getId()).build();
            }

            // 반복하지 않는 일정
            savedSchedule.updateNotRecurrencePrivateSchedule(organizerId, name, description,
                type, startInstant, endInstant, isPublic, color);
            return ScheduleUpdateResponse.builder().scheduleId(savedSchedule.getId()).build();
        }

        // 회의 일정인 경우 //
        // 기존 참석자 명단
        List<Attendee> originalAttendeeList = attendeeRepository.findBySchedule(
            savedSchedule);
        List<Integer> originalAttendeeIdList = new ArrayList<>();
        for (Attendee originalAttendee : originalAttendeeList) {
            originalAttendeeIdList.add(originalAttendee.getMemberId());
        }
        // 변경 후 참석자 명단
        List<Attendee> newAttendeeList = new ArrayList<>();
        for (ScheduleUpdateRequest.ScheduleAttendee attendee : attendeeList) {
            Attendee newAttendee = Attendee.builder().isRequired(attendee.getIsRequired())
                .memberId(attendee.getMemberId()).schedule(savedSchedule)
                .status(AttendeeStatus.PENDING).build();
            newAttendeeList.add(newAttendee);
        }
        List<Integer> newAttendeeIdList = new ArrayList<>();
        for (Attendee newAttendee : newAttendeeList) {
            newAttendeeIdList.add(newAttendee.getMemberId());
        }

        // 일정 업데이트
        savedSchedule.updateMeetingSchedule(name, description, type, startInstant,
            endInstant,
            isPublic, color, newAttendeeList);

        List<Integer> cancelMemberList = new ArrayList<>();
        List<Integer> maintainedMemberList = new ArrayList<>();
        List<Integer> addMemberList = new ArrayList<>();

        // 기존 멤버 -> 유지 or 새로운 멤버 체크
        for (Integer newAttendeeId : newAttendeeIdList) {
            if (originalAttendeeIdList.contains(newAttendeeId)) {
                maintainedMemberList.add(newAttendeeId);
            } else {
                addMemberList.add(newAttendeeId);
            }
        }
        // 기존 멤버 -> 삭제 체크
        for (Integer originalAttendeeId : originalAttendeeIdList) {
            if (!newAttendeeIdList.contains(originalAttendeeId)) {
                cancelMemberList.add(originalAttendeeId);
            }
        }
        // 참석자 명단 삭제
        attendeeRepository.deleteAll(originalAttendeeList);
        // 참석자 명단 추가
        attendeeRepository.saveAll(newAttendeeList);
        UserInfoResponse response = userServiceClient.getUserName(organizerId);
        MeetingUpdateMessage message = MeetingUpdateMessage.builder()
            .occurredAt(AlterTimeUtils.LocalDateTimeToUTCLocalDateTime(LocalDateTime.now(), zoneId))
            .scheduleId(scheduleId)
            .scheduleName(savedSchedule.getName())
            .organizerId(savedSchedule.getOrganizerId())
            .organizerName(response != null ? response.getName() : null)
            .previousStartTime(null)
            .previousEndTime(null)
            .updatedStartTime(
                AlterTimeUtils.InstantToUTCLocalDateTime(savedSchedule.getStartDatetime()))
            .updatedEndTime(
                AlterTimeUtils.InstantToUTCLocalDateTime(savedSchedule.getEndDatetime()))
            .maintainedAttendeeIds(maintainedMemberList)
            .addedAttendeeIds(addMemberList)
            .removedAttendeeIds(cancelMemberList)
            .updatedFields(null)
            .build();

        List<UpdatedField> updatedFields = new ArrayList<>();

        if (nameIsChanged != null && nameIsChanged) {
            updatedFields.add(UpdatedField.TITLE);
        }

        if (descriptionIsChanged != null && descriptionIsChanged) {
            updatedFields.add(UpdatedField.DESCRIPTION);
        }

        if (timeIsChanged != null && timeIsChanged) {
            updatedFields.add(UpdatedField.TIME);
            message.setPreviousStartTime(
                AlterTimeUtils.InstantToUTCLocalDateTime(savedSchedule.getStartDatetime()));
            message.setPreviousEndTime(
                AlterTimeUtils.InstantToUTCLocalDateTime(savedSchedule.getEndDatetime()));
            message.setUpdatedStartTime(AlterTimeUtils.InstantToUTCLocalDateTime(startInstant));
            message.setUpdatedEndTime(AlterTimeUtils.InstantToUTCLocalDateTime(endInstant));
        }

        message.setUpdatedFields(updatedFields);

        kafkaProducer.send("meeting-updated", message);
        return ScheduleUpdateResponse.builder().scheduleId(savedSchedule.getId()).build();
    }

    @Override
    @Transactional
    public void deleteSchedule(Integer memberId, Long id,
        ScheduleDeleteRequest scheduleDeleteRequest) {
        ZoneId zoneId = ZoneId.of(
            memberTimezoneRepository.findById(memberId).orElseThrow().getZoneId());
        ScheduleDeleteRange deleteRange = scheduleDeleteRequest.getDeleteRange();
        LocalDateTime deleteLocalDateTime = scheduleDeleteRequest.getDeleteDatetime();
        Instant deleteInstant =
            deleteLocalDateTime != null ? AlterTimeUtils.LocalDateTimeToInstant(
                deleteLocalDateTime,
                zoneId) : null;
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();

        // 주최자만 삭제 가능
        validateOrganizer(schedule, memberId);
        // 회의 일정인 경우
        if (schedule.getType().equals(ScheduleType.MEETING)) {
            // 참석자들
            List<Attendee> attendeeList = attendeeRepository.findBySchedule(schedule);
            // 일정 제안 및 참석자 삭제
            for (Attendee attendee : attendeeList) {
                if (attendee.getProposal() != null) {
                    proposalRepository.delete(attendee.getProposal());
                }
                attendeeRepository.delete(attendee);
            }
            // 일정 삭제
            scheduleRepository.delete(schedule);
            UserInfoResponse response = userServiceClient.getUserName(schedule.getOrganizerId());
            MeetingDeleteMessage message = MeetingDeleteMessage.builder()
                .occurredAt(
                    AlterTimeUtils.LocalDateTimeToUTCLocalDateTime(LocalDateTime.now(), zoneId))
                .scheduleId(id)
                .scheduleName(schedule.getName())
                .organizerId(schedule.getOrganizerId())
                .organizerName(response != null ? response.getName() : null)
                .startTime(AlterTimeUtils.InstantToUTCLocalDateTime(schedule.getStartDatetime()))
                .endTime(AlterTimeUtils.InstantToUTCLocalDateTime(schedule.getEndDatetime()))
                .attendeeIds(attendeeList.stream().map(Attendee::getMemberId).toList())
                .build();
            kafkaProducer.send("meeting-deleted", message);
            return;
        }

        // 회의 외의 일정인 경우
        switch (deleteRange) {
            // 1. 모든 일정 삭제하는 경우
            case ALL:
                List<Schedule> scheduleList = scheduleRepository.findScheduleByParentSchedule(
                    schedule);
                scheduleList.add(schedule);
                for (Schedule s : scheduleList) {
                    // 일정 반복 삭제
                    if (s.getRecurrence() != null) {
                        recurrenceRepository.delete(s.getRecurrence());
                    }
                    // 부모 일정 유무 체크
                    if (s.getSchedule() != null && !scheduleList.contains(s.getSchedule())) {
                        scheduleList.add(s.getSchedule());
                    }
                }
                // 일정 삭제
                scheduleRepository.deleteAll(scheduleList);
                break;
            // 2. 선택적으로 삭제하는 경우
            case ONE:
                scheduleRepository.save(Schedule.builder().name(schedule.getName())
                    .organizerId(schedule.getOrganizerId())
                    .description(schedule.getDescription())
                    .type(schedule.getType()).startDatetime(deleteInstant)
                    .endDatetime(deleteInstant).isPublic(schedule.getIsPublic())
                    .color(schedule.getColor()).isDeleted(true).schedule(schedule).build());
                break;
            // 3. 해당일자부터 모두 삭제하는 경우
            default:
                Recurrence recurrence = schedule.getRecurrence();
                recurrence.terminateRecurrenceByDate(deleteLocalDateTime);
                break;
        }
    }

    @Override
    @Transactional
    public void decideAttendance(Long scheduleId, Integer memberId,
        DecideAttendanceRequest decideAttendanceRequest) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
        Attendee attendee = attendeeRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
            .orElseThrow();
        String reason = decideAttendanceRequest.getReason();
        ZoneId zoneId = ZoneId.of(
            memberTimezoneRepository.findById(memberId).orElseThrow().getZoneId());
        AttendeeStatus status = decideAttendanceRequest.getStatus();

        attendee.updateStatus(status, reason);
        UserInfoResponse response = userServiceClient.getUserName(memberId);
        AttendeeResponseMessage message = AttendeeResponseMessage.builder()
            .occurredAt(AlterTimeUtils.LocalDateTimeToUTCLocalDateTime(LocalDateTime.now(), zoneId))
            .scheduleId(scheduleId)
            .scheduleName(schedule.getName())
            .organizerId(schedule.getOrganizerId())
            .attendeeName(response != null ? response.getName() : null)
            .build();

        // 수락시
        if (status.equals(AttendeeStatus.ACCEPTED)) {
            message.setResponse(Response.ACCEPTED);
            // 거절시
        } else if (status.equals(AttendeeStatus.DECLINED)) {
            message.setResponse(Response.DECLINED);
        }

        kafkaProducer.send("attendee-response", message);

        if (decideAttendanceRequest.getStartDatetime() != null) {
            Proposal proposal = Proposal.builder()
                .startDatetime(AlterTimeUtils.LocalDateTimeToInstant(
                    decideAttendanceRequest.getStartDatetime(), zoneId))
                .endDatetime(AlterTimeUtils.LocalDateTimeToInstant(
                    decideAttendanceRequest.getEndDatetime(), zoneId))
                .build();
            Proposal savedProposal = proposalRepository.save(proposal);
            attendee.updateProposal(savedProposal);

            AttendeeProposalMessage proposalMessage = AttendeeProposalMessage.builder()
                .occurredAt(
                    AlterTimeUtils.LocalDateTimeToUTCLocalDateTime(LocalDateTime.now(), zoneId))
                .scheduleId(scheduleId)
                .scheduleName(schedule.getName())
                .organizerId(schedule.getOrganizerId())
                .attendeeName(response != null ? response.getName() : null)
                .proposedStartTime(AlterTimeUtils.LocalDateTimeToUTCLocalDateTime(
                    decideAttendanceRequest.getStartDatetime(), zoneId))
                .proposedEndTime(AlterTimeUtils.LocalDateTimeToUTCLocalDateTime(
                    decideAttendanceRequest.getEndDatetime(), zoneId))
                .reason(decideAttendanceRequest.getReason())
                .build();
            kafkaProducer.send("attendee-proposal", proposalMessage);
        }
    }

    @Override
    @Transactional
    public void responseScheduleProposal(Long scheduleId, Integer memberId, Long proposalId,
        ResponseScheduleProposal responseScheduleProposal) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
        // 주최자가 아니면 에러
        validateOrganizer(schedule, memberId);
        // 기존 회의 시간
        Instant originalStartInstant = schedule.getStartDatetime();
        Instant originalEndInstant = schedule.getEndDatetime();
        // 기존 멤버
        List<Attendee> attendeeList = attendeeRepository.findBySchedule(schedule);

        // 수락여부
        Boolean isAccept = responseScheduleProposal.getIsAccepted();
        Proposal proposal = proposalRepository.findById(proposalId).orElseThrow();
        // 제안 거절시
        if (!isAccept) {
            attendeeRepository.deleteOneProposalByProposal(proposal);
            proposalRepository.delete(proposal);
            // 제안 수락시
        } else {
            // 회의 시간 수정
            Instant startInstant = proposal.getStartDatetime();
            Instant endInstant = proposal.getEndDatetime();
            schedule.changeScheduleTime(startInstant, endInstant);
            // 회의 주체자 이름 
            UserInfoResponse response = userServiceClient.getUserName(memberId);
            MeetingUpdateMessage message = MeetingUpdateMessage.builder()
                .occurredAt(AlterTimeUtils.LocalDateTimeToUTCLocalDateTime(LocalDateTime.now(),
                    ZoneId.of(
                        memberTimezoneRepository.findById(memberId).orElseThrow().getZoneId())))
                .scheduleId(scheduleId)
                .scheduleName(schedule.getName())
                .organizerId(schedule.getOrganizerId())
                .organizerName(response.getName())
                .previousStartTime(AlterTimeUtils.InstantToUTCLocalDateTime(originalStartInstant))
                .previousEndTime(AlterTimeUtils.InstantToUTCLocalDateTime(originalEndInstant))
                .updatedStartTime(AlterTimeUtils.InstantToUTCLocalDateTime(startInstant))
                .updatedEndTime(AlterTimeUtils.InstantToUTCLocalDateTime(endInstant))
                .maintainedAttendeeIds(attendeeList.stream().map(Attendee::getMemberId).toList())
                .updatedFields(List.of(UpdatedField.TIME))
                .build();
            // 수정 사항 전송
            kafkaProducer.send("meeting-updated", message);
            // 나머지 제안 다 삭제
            attendeeRepository.deleteAllProposalBySchedule(schedule);
            for (Attendee a : attendeeList) {
                if (a.getProposal() != null) {
                    Proposal proposals = a.getProposal();
                    proposalRepository.delete(proposals);
                }
            }
        }
    }

    // 주최자와 일치하는지 판단
    public void validateOrganizer(Schedule schedule, Integer memberId) {
        if (!Objects.equals(schedule.getOrganizerId(), memberId)) {
            throw ErrorCode.SCHEDULE_UPDATE_NO_QUALIFICATION_ERROR.build();
        }
    }

    // 삭제된 일정 체크하기
    public boolean isDeletedSchedule(List<DeletedSchedule> deleteScheduleList, Schedule
        s,
        ZoneId zoneId, LocalDateTime startLocalDatetime) {
        for (DeletedSchedule d : deleteScheduleList) {
            if (AlterTimeUtils.instantToLocalDateTime(
                    d.getDeleteDateInstant(),
                    zoneId).toLocalDate()
                .equals(startLocalDatetime.toLocalDate()) && d.getParentScheduleId()
                .equals(s.getId())) {
                deleteScheduleList.remove(d);
                return true;
            }
        }
        return false;
    }

    // 수정된 일정 체크하기
    public boolean isUpdatedSchedule(List<UpdatedSchedule> updatedScheduleList, Schedule
        s,
        ZoneId zoneId, LocalDateTime startDatetime) {
        for (UpdatedSchedule u : updatedScheduleList) {
            if (AlterTimeUtils.instantToLocalDateTime(u.getUpdateDateInstant(),
                zoneId).toLocalDate().equals(
                startDatetime.toLocalDate()) && u.getParentScheduleId()
                .equals(s.getId())) {
                return true;
            }
        }
        return false;
    }

    // 조회 기간을 벗어나는 경우 체크하기
    public boolean isOutOfPeriod(LocalDateTime startLocalDatetime, Instant startInstant,
        LocalDateTime endLocalDatetime, Instant endInstant,
        ZoneId zoneId) {
        return !AlterTimeUtils.LocalDateTimeToInstant(startLocalDatetime, zoneId)
            .isBefore(endInstant) || !AlterTimeUtils.LocalDateTimeToInstant(
            endLocalDatetime, zoneId).isAfter(startInstant);
    }

    // 주 반복 요일 순서 정렬하기
    public List<Integer> sortDayList(EnumSet<RecurrenceDayType> recurrenceDay, DayOfWeek
        startDay) {
        List<Integer> dayList = new ArrayList<>();
        PriorityQueue<Integer> dayQueue = new PriorityQueue<>();
        for (RecurrenceDayType day : recurrenceDay) {
            dayQueue.add(day.ordinal() - startDay.ordinal() < 0 ?
                day.ordinal() - startDay.ordinal() + 7
                : day.ordinal() - startDay.ordinal());
        }
        while (!dayQueue.isEmpty()) {
            dayList.add(dayQueue.poll());
        }
        return dayList;
    }
}
