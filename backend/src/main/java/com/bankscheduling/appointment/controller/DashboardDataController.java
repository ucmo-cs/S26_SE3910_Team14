package com.bankscheduling.appointment.controller;

import com.bankscheduling.appointment.dto.appointment.StaffAppointmentUpdateRequest;
import com.bankscheduling.appointment.dto.dashboard.DashboardActivityDto;
import com.bankscheduling.appointment.dto.dashboard.DashboardAppointmentDto;
import com.bankscheduling.appointment.dto.dashboard.DashboardUserDto;
import com.bankscheduling.appointment.service.DashboardDataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardDataController {
    private final DashboardDataService dashboardDataService;

    public DashboardDataController(DashboardDataService dashboardDataService) {
        this.dashboardDataService = dashboardDataService;
    }

    @GetMapping("/appointments/recent")
    public List<DashboardAppointmentDto> recentAppointments(@RequestParam(defaultValue = "25") int limit) {
        return dashboardDataService.getRecentAppointments(limit);
    }

    @GetMapping("/activity/recent")
    public List<DashboardActivityDto> recentActivity(@RequestParam(defaultValue = "50") int limit) {
        return dashboardDataService.getRecentActivity(limit);
    }

    @GetMapping("/users")
    public List<DashboardUserDto> users() {
        return dashboardDataService.getUsers();
    }

    @PatchMapping("/users/{accountId}/lock")
    public void setLock(@PathVariable Long accountId, @RequestBody LockRequest request) {
        dashboardDataService.setUserLock(accountId, request.locked());
    }

    @PatchMapping("/appointments/{appointmentId}")
    public DashboardAppointmentDto updateAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody StaffAppointmentUpdateRequest request
    ) {
        return dashboardDataService.updateAppointmentForStaff(appointmentId, request);
    }

    @DeleteMapping("/appointments/{appointmentId}")
    public void deleteAppointment(@PathVariable Long appointmentId) {
        dashboardDataService.deleteAppointmentForStaff(appointmentId);
    }

    public record LockRequest(boolean locked) {
    }
}
