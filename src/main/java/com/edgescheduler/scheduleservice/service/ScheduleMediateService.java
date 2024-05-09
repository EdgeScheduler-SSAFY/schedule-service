package com.edgescheduler.scheduleservice.service;

import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityRequest;
import com.edgescheduler.scheduleservice.dto.request.CalculateAvailabilityWithProposalRequest;
import com.edgescheduler.scheduleservice.dto.response.CalculateAvailabilityResponse;

public interface ScheduleMediateService {

    Object calculateAvailableMembersWithProposedSchedule(
        CalculateAvailabilityWithProposalRequest calculateAvailabilityWithProposalRequest);

    CalculateAvailabilityResponse calculateAvailability(
        CalculateAvailabilityRequest calculateAvailabilityRequest);
}
