package com.bankscheduling.appointment.dto.publicbooking;

public record PublicBranchDto(
        Long id,
        String displayName,
        String streetLine1,
        String streetLine2,
        String city,
        String stateProvince,
        String postalCode,
        String timeZone
) {
}
