package com.bankscheduling.appointment.controller;

import com.bankscheduling.appointment.dto.customerauth.CustomerAppointmentDto;
import com.bankscheduling.appointment.dto.customerauth.CustomerProfileDto;
import com.bankscheduling.appointment.service.CustomerAuthService;
import org.springframework.web.bind.annotation.GetMapping;
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
    public List<CustomerAppointmentDto> appointments() {
        return customerAuthService.getCurrentCustomerAppointments();
    }
}
