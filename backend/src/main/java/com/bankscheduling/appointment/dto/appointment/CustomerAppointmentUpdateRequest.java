package com.bankscheduling.appointment.dto.appointment;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CustomerAppointmentUpdateRequest(
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        String notes
) {
}
