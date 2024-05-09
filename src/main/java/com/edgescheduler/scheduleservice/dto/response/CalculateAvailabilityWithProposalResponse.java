package com.edgescheduler.scheduleservice.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CalculateAvailabilityWithProposalResponse {

    private List<AvailableMember> availableMembers;
    private List<AvailableMember> unavailableMembers;

    @Getter
    @Builder
    public static class AvailableMember {

        private Integer memberId;
        private String memberName;
        private Boolean isRequired;
    }
}
