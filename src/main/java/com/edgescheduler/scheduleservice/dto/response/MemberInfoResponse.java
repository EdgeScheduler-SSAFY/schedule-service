package com.edgescheduler.scheduleservice.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class MemberInfoResponse {

    private Integer id;
    private Integer profile;
    private String name;
    private RoleResponse role;
    private String email;
    private String department;
    private String region;
    private String zoneId;

}
