package com.bankscheduling.appointment.controller;

import com.bankscheduling.appointment.dto.appointment.AppointmentResponseDto;
import com.bankscheduling.appointment.service.AppointmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/branch/{branchId}")
    public List<AppointmentResponseDto> listForBranch(@PathVariable Long branchId) {
        return appointmentService.getAppointmentsForBranch(branchId);
    }
}
