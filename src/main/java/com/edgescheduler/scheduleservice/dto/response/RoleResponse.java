package com.edgescheduler.scheduleservice.dto.response;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RoleResponse {
    USER("USER"),
    ADMIN("ADMIN");

    private final String role;
}
