package com.bankscheduling.appointment.dto.customerauth;

public record CustomerProfileDto(
        Long id,
        String fullName,
        String email
) {
}
