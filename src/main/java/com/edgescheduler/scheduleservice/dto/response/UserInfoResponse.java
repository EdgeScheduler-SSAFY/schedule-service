package com.edgescheduler.scheduleservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponse {
    private Integer id;
    private Integer profile;
    private String name;
    private RoleResponse role;
    private String email;
    private String department;
    private String region;
    private String zoneId;
}
