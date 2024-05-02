package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.domain.Attendee;
import com.edgescheduler.scheduleservice.domain.AttendeeStatus;
import com.edgescheduler.scheduleservice.domain.Recurrence;
import com.edgescheduler.scheduleservice.domain.RecurrenceFreqType;
import com.edgescheduler.scheduleservice.domain.Schedule;
import com.edgescheduler.scheduleservice.domain.ScheduleType;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleCreateRequest.ScheduleAttendee;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest;
import com.edgescheduler.scheduleservice.dto.request.ScheduleUpdateRequest.RecurrenceDetails;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse.IndividualSchedules;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse.TokenizedTimeAvailability;
import com.edgescheduler.scheduleservice.dto.response.ScheduleCreateResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse.ScheduleDetailAttendee;
import com.edgescheduler.scheduleservice.dto.response.ScheduleDetailReadResponse.ScheduleProposal;
import com.edgescheduler.scheduleservice.dto.response.ScheduleListReadResponse;
import com.edgescheduler.scheduleservice.dto.response.ScheduleUpdateResponse;
import com.edgescheduler.scheduleservice.exception.ErrorCode;
import com.edgescheduler.scheduleservice.repository.AttendeeRepository;
import com.edgescheduler.scheduleservice.repository.MemberTimezoneRepository;
import com.edgescheduler.scheduleservice.repository.RecurrenceRepository;
import com.edgescheduler.scheduleservice.repository.ScheduleRepository;
import com.edgescheduler.scheduleservice.util.AlterTimeUtils;
import com.edgescheduler.scheduleservice.util.TimeIntervalUtils;
import com.edgescheduler.scheduleservice.vo.ScheduleVO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimpleScheduleService implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMediateService scheduleMediateService;
    private final MemberTimezoneRepository memberTimezoneRepository;
    private final AttendeeRepository attendeeRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final AlterTimeUtils alterTimeUtils;

    @Override
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
            .orElseThrow(() -> new IllegalArgumentException("null"))
            .getZoneId());
        Instant startDatetimeInstant = AlterTimeUtils.LocalDateTimeToInstant(startDatetime, zoneId
        );
        Instant endDatetimeInstant = AlterTimeUtils.LocalDateTimeToInstant(endDatetime, zoneId);

        Schedule schedule = Schedule.builder()
            .organizerId(organizerId)
            .name(name)
            .description(description)
            .type(type)
            .color(color)
            .startDatetime(startDatetimeInstant)
            .endDatetime(endDatetimeInstant)
            .isPublic(isPublic)
            .build();
        Schedule saveSchedule = scheduleRepository.save(schedule);

        // 반복되는 일정인 경우
        if (isRecurrence) {
            Instant expiredDate = null;
            LocalDateTime expiredDateInput = scheduleCreateRequest.getRecurrence().getExpiredDate();
            // 기한이 존재하는 반복 일정의 경우
            if (expiredDateInput != null) {
                expiredDate = AlterTimeUtils.LocalDateTimeToInstant(expiredDateInput, zoneId
                );
            }

            Set<String> recurrenceDay = new TreeSet<>();
            // 반복 요일이 존재하는 일정의 경우
            if (!scheduleCreateRequest.getRecurrence().getRecurrenceDay().isEmpty()) {
                recurrenceDay = new TreeSet<>(
                    scheduleCreateRequest.getRecurrence().getRecurrenceDay());
            }

            Recurrence recurrence = Recurrence.builder()
                .count(scheduleCreateRequest.getRecurrence().getCount())
                .freq(RecurrenceFreqType.valueOf(scheduleCreateRequest.getRecurrence().getFreq()))
                .intv(scheduleCreateRequest.getRecurrence().getIntv())
                .expiredDate(expiredDate)
                .recurrenceDay(recurrenceDay)
                .build();

            Recurrence savedRecurrence = recurrenceRepository.save(recurrence);

            updateScheduleRecurrence(saveSchedule.getId(), savedRecurrence);
        }
        // 회의 일정의 경우
        if (type.equals(ScheduleType.MEETING)) {
            List<ScheduleAttendee> attendeeList = scheduleCreateRequest.getAttendeeList();
            if (!attendeeList.isEmpty()) {
                attendeeList.stream()
                    .map(attendee -> Attendee.builder()
                        .isRequired(attendee.getIsRequired())
                        .memberId(attendee.getMemberId())
                        .status(AttendeeStatus.PENDING)
                        .schedule(saveSchedule)
                        .build())
                    .forEach(attendeeRepository::save);
            }
        }
        return ScheduleCreateResponse.builder()
            .scheduleId(saveSchedule.getId())
            .build();
    }

    @Override
    public ScheduleDetailReadResponse getSchedule(Integer memberId, Long id) {
        // 해당 일정 조회
        Schedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("null"));
        // 조회하는 사람 기준의 시간대
        ZoneId zoneId = ZoneId.of(memberTimezoneRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("null"))
            .getZoneId());
        List<Attendee> attendees = attendeeRepository.findBySchedule(schedule);
        List<ScheduleDetailAttendee> attendeeList = new ArrayList<>();
        // 일정 공유하는 사람이 있는 경우
        if (!attendees.isEmpty()) {
            for (Attendee attendee : attendees) {
                ScheduleProposal scheduleProposal = null;
                // 시간 제안한 경우가 있는 경우
                if (attendee.getProposal() != null) {
                    scheduleProposal = ScheduleProposal.builder()
                        .proposalId(attendee.getProposal().getId())
                        .startDatetime(AlterTimeUtils.instantToLocalDateTime(
                            attendee.getProposal().getStartDatetime(), zoneId))
                        .endDatetime(AlterTimeUtils.instantToLocalDateTime(attendee.getProposal()
                            .getEndDatetime(), zoneId))
                        .build();
                }
                ScheduleDetailAttendee attendeeDetail = ScheduleDetailAttendee.builder()
                    .memberId(attendee.getId())
                    .isRequired(attendee.getIsRequired())
                    .status(attendee.getStatus())
                    .reason(attendee.getReason())
                    .proposal(scheduleProposal)
                    .build();

                attendeeList.add(attendeeDetail);
            }
        }

        ScheduleDetailReadResponse.RecurrenceDetails recurrenceDetails = null;
        if (schedule.getRecurrence() != null) {
            LocalDateTime expiredDatetime = AlterTimeUtils.instantToLocalDateTime(
                schedule.getRecurrence().getExpiredDate(), zoneId);
            recurrenceDetails = ScheduleDetailReadResponse.RecurrenceDetails.builder()
                .count(schedule.getRecurrence().getCount())
                .intv(schedule.getRecurrence().getIntv())
                .expiredDate(expiredDatetime)
                .recurrenceDay(new ArrayList<>(schedule.getRecurrence().getRecurrenceDay()))
                .freq(String.valueOf(schedule.getRecurrence().getFreq()))
                .build();
        }

        return ScheduleDetailReadResponse.builder()
            .scheduleId(schedule.getId())
            .organizerId(schedule.getOrganizerId())
            .name(schedule.getName())
            .description(schedule.getDescription())
            .type(schedule.getType())
            .color(schedule.getColor())
            .startDatetime(
                AlterTimeUtils.instantToLocalDateTime(schedule.getStartDatetime(), zoneId))
            .endDatetime(AlterTimeUtils.instantToLocalDateTime(schedule.getEndDatetime(), zoneId))
            .isPublic(schedule.getIsPublic())
            .attendeeList(attendeeList)
            .recurrenceDetails(recurrenceDetails)
            .build();
    }

    @Override
    public ScheduleListReadResponse getScheduleByPeriod(LocalDateTime start, LocalDateTime end) {
        return null;
    }

    @Override
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
        Boolean attendeeIsChanged = scheduleRequest.getAttendeeIsChanged();

        ZoneId zoneId = ZoneId.of(memberTimezoneRepository.findById(memberId).orElseThrow()
            .getZoneId());
        Instant startInstant = AlterTimeUtils.LocalDateTimeToInstant(startDatetime, zoneId);
        Instant endInstant = AlterTimeUtils.LocalDateTimeToInstant(endDatetime, zoneId);

        Schedule savedSchedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("null"));
        Integer organizerId = savedSchedule.getOrganizerId();
        // 주최자가 아닌 경우 오류
        if (!Objects.equals(savedSchedule.getOrganizerId(), memberId)) {
            throw ErrorCode.SCHEDULE_UPDATE_NO_QUALIFICATION_ERROR.build();
        }

        // 회의 외의 일정인 경우
        if (!String.valueOf(savedSchedule.getType()).equals("MEETING")) {
            // 반복 일정 & 해당 이벤트만 수정
            if (isRecurrence && isOneOff) {
                Schedule modifiedSchedule = Schedule.builder()
                    .organizerId(organizerId)
                    .name(name)
                    .description(description)
                    .type(type)
                    .startDatetime(startInstant)
                    .endDatetime(endInstant)
                    .isPublic(isPublic)
                    .color(color)
                    .schedule(savedSchedule)
                    .build();
                scheduleRepository.save(modifiedSchedule);
            }
            // 반복 일정 & 이후 모든 이벤트 수정
            if (isRecurrence && !isOneOff) {
                // 기본 반복 기한 오늘 날짜로 수정하기
                terminateRecurrence(savedSchedule.getRecurrence(), zoneId);
                // 새로운 반복 일정 추가하기
                String freq = recurrence.getFreq();
                Integer intv = recurrence.getIntv();
                LocalDateTime expiredDate = recurrence.getExpiredDate();
                Instant expiredInstant = null;
                if (expiredDate != null) {
                    expiredInstant = AlterTimeUtils.LocalDateTimeToInstant(expiredDate,
                        zoneId);
                }
                Integer count = recurrence.getCount();
                Set<String> recurrenceDay;
                if (recurrence.getRecurrenceDay() != null) {
                    recurrenceDay = new TreeSet<>(recurrence.getRecurrenceDay());
                } else {
                    recurrenceDay = new TreeSet<>();
                }
                Recurrence newRecurrence = Recurrence.builder()
                    .freq(freq != null ? RecurrenceFreqType.valueOf(freq) : null)
                    .intv(intv)
                    .expiredDate(expiredInstant)
                    .count(count)
                    .recurrenceDay(recurrenceDay)
                    .build();

                // 일정 추가하기
                Schedule modifiedSchedule = Schedule.builder()
                    .organizerId(organizerId)
                    .name(name)
                    .description(description)
                    .type(type)
                    .startDatetime(startInstant)
                    .endDatetime(endInstant)
                    .isPublic(isPublic)
                    .color(color)
                    .recurrence(newRecurrence)
                    .build();
                scheduleRepository.save(modifiedSchedule);
            }

            // 반복하지 않는 일정
            if (!isRecurrence) {
                savedSchedule.updateNotRecurrencePrivateSchedule(organizerId, name, description,
                    type,
                    startInstant, endInstant, isPublic, color);
            }
        }

        // 회의 일정인 경우
        if (String.valueOf(savedSchedule.getType()).equals("MEETING")) {
            // 기존 참석자 명단
            List<Attendee> originalAttendeeList = attendeeRepository.findBySchedule(savedSchedule);
            List<Integer> originalAttendeeIdList = new ArrayList<>();
            for (Attendee originalAttendee : originalAttendeeList) {
                originalAttendeeIdList.add(originalAttendee.getMemberId());
            }
            // 변경 후 참석자 명단
            List<Attendee> newAttendeeList = new ArrayList<>();
            for (ScheduleUpdateRequest.ScheduleAttendee attendee : attendeeList) {
                Attendee newAttendee = Attendee.builder()
                    .isRequired(attendee.getIsRequired())
                    .memberId(attendee.getMemberId())
                    .schedule(savedSchedule)
                    .status(AttendeeStatus.PENDING)
                    .build();
                newAttendeeList.add(newAttendee);
            }
            List<Integer> newAttendeeIdList = new ArrayList<>();
            for (Attendee newAttendee : newAttendeeList) {
                newAttendeeIdList.add(newAttendee.getMemberId());
            }

            // 일정 업데이트
            updateMeetingSchedule(savedSchedule, name,
                description, type, startInstant, endInstant, isPublic, color, newAttendeeList);

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

        }
        return ScheduleUpdateResponse.builder().scheduleId(savedSchedule.getId()).build();
    }

    @Override
    public void deleteSchedule(Long id) {

    }

    // TODO: 일정 가용성 계산이 동일한 List를 반복적으로 순회하도록 되어있음. 최적화 필요.
    @Override
    public CalculateAvailabilityResponse calculateAvailability(
        CalculateAvailabilityRequest calculateAvailabilityRequest) {

        // 주어진 기한 조건의 시작 일시와 끝 일시를 15분 단위(00분 / 15분 / 30분 / 45분)로 조정하고 그 사이의 토큰화된 구간의 개수를 계산한다.
        LocalDateTime adjustedStart = TimeIntervalUtils.adjustToNextQuarterHour(
            calculateAvailabilityRequest.getStartDatetime());
        LocalDateTime adjustedEnd = TimeIntervalUtils.adjustToPreviousQuarterHour(
            calculateAvailabilityRequest.getEndDatetime());
        long intervalCount = TimeIntervalUtils.calculateIntervalCount(adjustedStart, adjustedEnd);

        // 조정된 시작 일시와 끝 일시를 UTC 표준시로 변환한다
        Instant start = adjustedStart.toInstant(ZoneOffset.UTC);
        Instant end = adjustedEnd.toInstant(ZoneOffset.UTC);

        // 필참 멤버와 그외 멤버의 스케줄을 조회하여 Map 형태로 저장한다
        Map<Integer, List<ScheduleVO>> requiredMemberScheduleMap = new HashMap<>();
        Map<Integer, List<ScheduleVO>> unrequiredMemberScheduleMap = new HashMap<>();
        calculateAvailabilityRequest.getMemberList()
            .forEach(
                member -> {
                    if (member.getIsRequired()) {
                        requiredMemberScheduleMap.put(
                            member.getMemberId(),
                            findSchedulesForAttendeeWithinPeriod(member.getMemberId(), start, end)
                        );
                    } else {
                        unrequiredMemberScheduleMap.put(
                            member.getMemberId(),
                            findSchedulesForAttendeeWithinPeriod(member.getMemberId(), start, end)
                        );
                    }
                }
            );

        // 응답에 필요한 개별 회원의 스케줄 목록을 생성한다
        List<IndividualSchedules> schedules = new ArrayList<>();
        addIndividualSchedules(schedules, requiredMemberScheduleMap);
        addIndividualSchedules(schedules, unrequiredMemberScheduleMap);

        // 토큰화된 시간대별로 필참 멤버와 그외 멤버의 가용성을 계산한다
        List<TokenizedTimeAvailability> tokenizedTimeAvailabilities = new ArrayList<>();

        for (long i = 0; i < intervalCount; i++) {

            Instant intervalStart = start.plusSeconds(i * 900);
            Instant intervalEnd = start.plusSeconds((i + 1) * 900);

            int availableRequiredMemberCount = (int) requiredMemberScheduleMap.values().stream()
                .filter(scheduleVOList -> scheduleMediateService.isAvailableWithOtherSchedule(
                    intervalStart, intervalEnd, scheduleVOList))
                .count();
            int availableMemberCount =
                availableRequiredMemberCount + (int) unrequiredMemberScheduleMap.values().stream()
                    .filter(scheduleVOList -> scheduleMediateService.isAvailableWithOtherSchedule(
                        intervalStart, intervalEnd, scheduleVOList))
                    .count();
            int availableRequiredMemberInWorkingHourCount = (int) requiredMemberScheduleMap.values()
                .stream()
                .filter(scheduleVOList -> scheduleMediateService.isOnWorkingHourAndAvailable(
                    intervalStart, intervalEnd, scheduleVOList))
                .count();
            int availableMemberInWorkingHourCount = availableRequiredMemberInWorkingHourCount
                + (int) unrequiredMemberScheduleMap.values().stream()
                .filter(scheduleVOList -> scheduleMediateService.isOnWorkingHourAndAvailable(
                    intervalStart, intervalEnd, scheduleVOList))
                .count();

            tokenizedTimeAvailabilities.add(
                TokenizedTimeAvailability.builder()
                    .availableMemberCount(availableMemberCount)
                    .availableRequiredMemberCount(availableRequiredMemberCount)
                    .availableMemberInWorkingHourCount(availableMemberInWorkingHourCount)
                    .availableRequiredMemberInWorkingHourCount(
                        availableRequiredMemberInWorkingHourCount)
                    .build()
            );
        }

        return CalculateAvailabilityResponse.builder()
            .schedules(schedules)
            .tokenizedTimeAvailabilities(tokenizedTimeAvailabilities)
            .build();
    }

    /**
     * 개별 회원의 스케줄을 추가한다.
     *  TODO: 추후 각 멤버별 시간대 변환 처리 필요
     */
    private void addIndividualSchedules(
        List<IndividualSchedules> schedules,
        Map<Integer, List<ScheduleVO>> memberScheduleMap
    ) {
        memberScheduleMap.keySet()
            .forEach(
                memberId -> {
                    schedules.add(
                        IndividualSchedules.builder()
                            .memberId(memberId)
                            .schedules(memberScheduleMap.get(memberId).stream().map(
                                schedule -> CalculateAvailabilityResponse.ScheduleEntry.builder()
                                    .name(schedule.name())
                                    .startDatetime(LocalDateTime.ofInstant(schedule.startDatetime(),
                                        ZoneOffset.UTC))
                                    .endDatetime(LocalDateTime.ofInstant(schedule.endDatetime(),
                                        ZoneOffset.UTC))
                                    .type(schedule.type())
                                    .isPublic(schedule.isPublic())
                                    .build()
                            ).toList())
                            .build()
                    );
                }
            );
    }

    private List<ScheduleVO> findSchedulesForAttendeeWithinPeriod(Integer memberId, Instant start,
        Instant end) {
        List<Schedule> schedules = scheduleRepository.findAcceptedSchedulesByAttendeeIdAndEndDatetimeBeforeAndStartDatetimeAfter(
            memberId,
            start,
            end
        );

        return schedules.stream()
            .map(schedule -> ScheduleVO.builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .type(schedule.getType())
                .startDatetime(schedule.getStartDatetime())
                .endDatetime(schedule.getEndDatetime())
                .isPublic(schedule.getIsPublic())
                .build()
            )
            .toList();
    }

    // 반복일정 추가하기
    public void updateScheduleRecurrence(Long id, Recurrence recurrence) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        schedule.setRecurrence(recurrence);
        scheduleRepository.saveAndFlush(schedule);
    }

    public void terminateRecurrence(Recurrence recurrence, ZoneId zoneId) {
        recurrence.terminateRecurrence(zoneId);
        recurrenceRepository.saveAndFlush(recurrence);
    }

    public void updateMeetingSchedule(Schedule schedule,  String name,
        String description, ScheduleType type, Instant startInstant, Instant endInstant,
        Boolean isPublic, Integer color, List<Attendee> newAttendeeList) {
        schedule.updateMeetingSchedule( name,
            description, type, startInstant, endInstant, isPublic, color, newAttendeeList);
        scheduleRepository.saveAndFlush(schedule);
    }
}
