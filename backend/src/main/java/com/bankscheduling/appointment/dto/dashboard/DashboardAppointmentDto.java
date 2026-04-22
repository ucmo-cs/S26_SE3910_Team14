package com.bankscheduling.appointment.dto.dashboard;

import java.time.Instant;

public record DashboardAppointmentDto(
        Long id,
        String customer,
        String service,
        String branch,
        String status,
        Instant scheduledAt
) {
}
