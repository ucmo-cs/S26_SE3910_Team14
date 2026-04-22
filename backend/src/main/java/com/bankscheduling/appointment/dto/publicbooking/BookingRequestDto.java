package com.bankscheduling.appointment.dto.publicbooking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRequestDto(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotNull Long branchId,
        @NotNull Long serviceTypeId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime
) {
}
