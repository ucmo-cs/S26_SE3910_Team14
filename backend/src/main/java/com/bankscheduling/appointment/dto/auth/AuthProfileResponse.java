package com.bankscheduling.appointment.dto.auth;

public record AuthProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String role
) {
}
