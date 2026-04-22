package com.bankscheduling.appointment.dto.customerauth;

public record CustomerAuthResponse(
        String token,
        CustomerProfileDto customer
) {
}
