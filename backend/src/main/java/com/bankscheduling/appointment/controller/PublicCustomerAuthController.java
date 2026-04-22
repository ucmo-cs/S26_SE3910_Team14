package com.bankscheduling.appointment.controller;

import com.bankscheduling.appointment.dto.customerauth.CustomerAuthRequest;
import com.bankscheduling.appointment.dto.customerauth.CustomerAuthResponse;
import com.bankscheduling.appointment.dto.customerauth.CustomerRegisterRequest;
import com.bankscheduling.appointment.service.CustomerAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/auth/customer")
public class PublicCustomerAuthController {
    private final CustomerAuthService customerAuthService;

    public PublicCustomerAuthController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    @PostMapping("/register")
    public CustomerAuthResponse register(@Valid @RequestBody CustomerRegisterRequest request) {
        return customerAuthService.register(request);
    }

    @PostMapping("/login")
    public CustomerAuthResponse login(@Valid @RequestBody CustomerAuthRequest request) {
        return customerAuthService.login(request);
    }
}
