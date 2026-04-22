package com.bankscheduling.appointment.dto.dashboard;

public record DashboardUserDto(
        Long id,
        String fullName,
        String email,
        String role,
        boolean locked
) {
}
