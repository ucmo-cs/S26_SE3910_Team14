package com.bankscheduling.appointment.dto.publicbooking;

import java.util.List;

public record PublicTimeslotsDto(
        String timeZone,
        int slotDurationMinutes,
        List<String> slots
) {
}
