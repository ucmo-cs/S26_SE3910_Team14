package com.bankscheduling.appointment.dto.appointment;

import java.time.Instant;

/**
 * API projection for appointments (no JPA entities exposed).
 */
public record AppointmentResponseDto(
        Long id,
        Long branchId,
        CustomerSummary customer,
        EmployeeSummary employee,
        ServiceTypeSummary serviceType,
        Instant scheduledStart,
        Instant scheduledEnd,
        String status,
        String notes,
        int optimisticLockVersion
) {

    public record CustomerSummary(Long id, String fullName, String email, String phone) {
    }

    public record EmployeeSummary(Long id, String firstName, String lastName) {
    }

    public record ServiceTypeSummary(Long id, String code, String displayName) {
    }
}
