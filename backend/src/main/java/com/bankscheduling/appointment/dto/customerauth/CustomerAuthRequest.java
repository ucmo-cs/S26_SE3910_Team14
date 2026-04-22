package com.bankscheduling.appointment.dto.customerauth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerAuthRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
