package com.bankscheduling.appointment.dto.publicbooking;

public record PublicServiceTypeDto(
        Long id,
        String code,
        String displayName,
        String description,
        int defaultDurationMinutes
) {
}
