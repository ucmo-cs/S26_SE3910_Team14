package com.bankscheduling.appointment.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

public record StaffAppointmentUpdateRequest(
        LocalDate date,
        LocalTime startTime,
        String status,
        String notes
) {
}
