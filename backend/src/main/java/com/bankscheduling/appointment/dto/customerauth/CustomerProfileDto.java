package com.bankscheduling.appointment.dto.customerauth;

public record CustomerProfileDto(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String role
) {
}
