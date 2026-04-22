package com.bankscheduling.appointment.dto.customerauth;

import java.time.Instant;

public record CustomerAppointmentDto(
        Long appointmentId,
        String topic,
        String branch,
        Instant scheduledStart,
        Instant scheduledEnd,
        String status
) {
}
