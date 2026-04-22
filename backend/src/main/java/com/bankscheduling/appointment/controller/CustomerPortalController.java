package com.bankscheduling.appointment.controller;

import com.bankscheduling.appointment.dto.appointment.CustomerAppointmentUpdateRequest;
import com.bankscheduling.appointment.dto.customerauth.CustomerAppointmentDto;
import com.bankscheduling.appointment.dto.customerauth.CustomerProfileDto;
import com.bankscheduling.appointment.service.CustomerAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerPortalController {
    private final CustomerAuthService customerAuthService;

    public CustomerPortalController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    @GetMapping("/me")
    public CustomerProfileDto me() {
        return customerAuthService.getCurrentCustomerProfile();
    }

    @GetMapping("/appointments")
    public List<CustomerAppointmentDto> appointments(@RequestParam(required = false) String status) {
        return customerAuthService.getCurrentCustomerAppointments(status);
    }

    @PutMapping("/appointments/{appointmentId}")
    public CustomerAppointmentDto updateAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CustomerAppointmentUpdateRequest request
    ) {
        return customerAuthService.updateCurrentCustomerAppointment(appointmentId, request);
    }

    @PatchMapping("/appointments/{appointmentId}/cancel")
    public void cancelAppointment(@PathVariable Long appointmentId) {
        customerAuthService.cancelCurrentCustomerAppointment(appointmentId);
    }
}
