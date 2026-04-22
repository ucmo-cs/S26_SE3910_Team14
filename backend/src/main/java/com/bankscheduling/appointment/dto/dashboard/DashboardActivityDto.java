package com.bankscheduling.appointment.dto.dashboard;

import java.time.Instant;

public record DashboardActivityDto(
        String id,
        Instant timestamp,
        String actionType,
        String user,
        String details
) {
}
