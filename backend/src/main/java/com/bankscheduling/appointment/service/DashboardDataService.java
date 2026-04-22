package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.appointment.StaffAppointmentUpdateRequest;
import com.bankscheduling.appointment.dto.dashboard.DashboardActivityDto;
import com.bankscheduling.appointment.dto.dashboard.DashboardAppointmentDto;
import com.bankscheduling.appointment.dto.dashboard.DashboardUserDto;
import com.bankscheduling.appointment.entity.Appointment;
import com.bankscheduling.appointment.entity.AppointmentStatus;
import com.bankscheduling.appointment.entity.AuditLog;
import com.bankscheduling.appointment.entity.BranchBusinessHours;
import com.bankscheduling.appointment.entity.CustomerAccount;
import com.bankscheduling.appointment.repository.AppointmentRepository;
import com.bankscheduling.appointment.repository.AuditLogRepository;
import com.bankscheduling.appointment.repository.BranchBusinessHoursRepository;
import com.bankscheduling.appointment.repository.CustomerAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class DashboardDataService {
    private final AppointmentRepository appointmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final CustomerAccountRepository customerAccountRepository;
    private final BranchBusinessHoursRepository branchBusinessHoursRepository;

    public DashboardDataService(
            AppointmentRepository appointmentRepository,
            AuditLogRepository auditLogRepository,
            CustomerAccountRepository customerAccountRepository,
            BranchBusinessHoursRepository branchBusinessHoursRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.customerAccountRepository = customerAccountRepository;
        this.branchBusinessHoursRepository = branchBusinessHoursRepository;
    }

    @Transactional(readOnly = true)
    public List<DashboardAppointmentDto> getRecentAppointments(int limit) {
        requireAnyRole("ROLE_EMPLOYEE", "ROLE_ADMIN");
        return appointmentRepository.findAllWithAssociationsOrderByScheduledStartDesc().stream()
                .limit(Math.max(1, limit))
                .map(a -> new DashboardAppointmentDto(
                        a.getId(),
                        a.getCustomer().getFullName(),
                        a.getServiceType().getDisplayName(),
                        a.getBranch().getDisplayName(),
                        a.getStatus().name(),
                        a.getScheduledStart()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardActivityDto> getRecentActivity(int limit) {
        requireAnyRole("ROLE_EMPLOYEE", "ROLE_ADMIN");
        return auditLogRepository.findAllWithActorOrderByCreatedAtDesc().stream()
                .limit(Math.max(1, limit))
                .map(item -> new DashboardActivityDto(
                        "audit-" + item.getId(),
                        item.getCreatedAt(),
                        item.getAction(),
                        formatAuditUser(item),
                        formatAuditDetails(item)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardUserDto> getUsers() {
        requireAnyRole("ROLE_EMPLOYEE", "ROLE_ADMIN");
        return customerAccountRepository.findAllWithCustomerAndRole().stream()
                .map(account -> new DashboardUserDto(
                        account.getId(),
                        account.getCustomer().getFullName(),
                        account.getCustomer().getEmail(),
                        normalizeRole(account.getRole().getName()),
                        !account.isActive()
                ))
                .toList();
    }

    @Transactional
    public void setUserLock(Long accountId, boolean locked) {
        requireAnyRole("ROLE_ADMIN");
        CustomerAccount account = customerAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
        account.setActive(!locked);
        customerAccountRepository.save(account);
    }

    @Transactional
    public DashboardAppointmentDto updateAppointmentForStaff(Long appointmentId, StaffAppointmentUpdateRequest request) {
        requireAnyRole("ROLE_EMPLOYEE", "ROLE_ADMIN");
        Appointment appointment = appointmentRepository.findByIdWithAssociations(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        ZoneId branchZone = ZoneId.of(appointment.getBranch().getTimeZone());
        ZonedDateTime start = ZonedDateTime.of(request.date(), request.startTime(), branchZone);
        int duration = appointment.getServiceType().getDefaultDurationMinutes();
        ZonedDateTime end = start.plusMinutes(duration);
        BranchBusinessHours hours = branchBusinessHoursRepository.findByBranchIdAndDayOfWeekAndActiveTrue(
                appointment.getBranch().getId(),
                request.date().getDayOfWeek().getValue()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is closed on the selected date"));
        if (request.startTime().isBefore(hours.getOpenTime()) || end.toLocalTime().isAfter(hours.getCloseTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is outside branch business hours");
        }
        validateNineToFiveWindow(request.startTime(), end.toLocalTime());
        validateSlotBoundary(request.startTime(), duration);
        if (start.toInstant().isBefore(java.time.Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book an appointment in the past");
        }
        if (appointmentRepository.existsActiveBranchServiceStartExcluding(
                appointment.getBranch().getId(),
                appointment.getServiceType().getId(),
                start.toInstant(),
                appointment.getId()
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }

        if (appointmentRepository.existsEmployeeOverlapExcluding(
                appointment.getEmployee().getId(),
                appointment.getId(),
                start.toInstant(),
                end.toInstant()
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }

        if (request.status() != null && !request.status().isBlank()) {
            try {
                AppointmentStatus targetStatus = AppointmentStatus.valueOf(request.status().trim().toUpperCase());
                validateStaffStatusTransition(appointment.getStatus(), targetStatus);
                appointment.setStatus(targetStatus);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid appointment status");
            }
        }
        appointment.setScheduledStart(start.toInstant());
        appointment.setScheduledEnd(end.toInstant());
        appointment.setNotes(request.notes());
        Appointment saved;
        try {
            saved = appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }
        return new DashboardAppointmentDto(
                saved.getId(),
                saved.getCustomer().getFullName(),
                saved.getServiceType().getDisplayName(),
                saved.getBranch().getDisplayName(),
                saved.getStatus().name(),
                saved.getScheduledStart()
        );
    }

    @Transactional
    public void deleteAppointmentForStaff(Long appointmentId) {
        requireAnyRole("ROLE_EMPLOYEE", "ROLE_ADMIN");
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        appointmentRepository.delete(appointment);
    }

    private static String normalizeRole(String raw) {
        if (raw == null) {
            return "CUSTOMER";
        }
        return raw.startsWith("ROLE_") ? raw.substring("ROLE_".length()) : raw;
    }

    private static String formatAuditUser(AuditLog item) {
        if (item.getActorEmployeeId() != null) {
            String username = item.getActorUsername() == null ? "unknown-user" : item.getActorUsername();
            String email = item.getActorEmail() == null ? "n/a" : item.getActorEmail();
            String role = item.getActorRole() == null ? "n/a" : item.getActorRole();
            return username + " (" + email + ", " + role + ", emp#" + item.getActorEmployeeId() + ")";
        }
        if (item.getPerformedBy() != null) {
            return item.getPerformedBy().getWorkEmail();
        }
        return "SYSTEM";
    }

    private static String formatAuditDetails(AuditLog item) {
        StringBuilder builder = new StringBuilder();
        builder.append(item.getEntityType()).append(":").append(item.getEntityId());
        if (item.getRequestMethod() != null || item.getRequestPath() != null) {
            builder.append(" | request=");
            builder.append(item.getRequestMethod() == null ? "N/A" : item.getRequestMethod());
            builder.append(" ").append(item.getRequestPath() == null ? "N/A" : item.getRequestPath());
        }
        if (item.getIpAddress() != null && !item.getIpAddress().isBlank()) {
            builder.append(" | ip=").append(item.getIpAddress());
        }
        if (item.getCorrelationId() != null && !item.getCorrelationId().isBlank()) {
            builder.append(" | corr=").append(item.getCorrelationId());
        }
        if (item.getUserAgent() != null && !item.getUserAgent().isBlank()) {
            builder.append(" | ua=").append(item.getUserAgent());
        }
        return builder.toString();
    }

    private static void requireAnyRole(String... allowed) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
        for (String role : allowed) {
            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(a -> role.equals(a.getAuthority()));
            if (hasRole) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    private void validateNineToFiveWindow(LocalTime start, LocalTime end) {
        if (start.isBefore(LocalTime.of(9, 0)) || end.isAfter(LocalTime.of(17, 0))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments must be between 9:00 AM and 5:00 PM");
        }
    }

    private void validateSlotBoundary(LocalTime startTime, int durationMinutes) {
        if (startTime.getSecond() != 0 || startTime.getNano() != 0 || startTime.getMinute() % 30 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointments must start on 30-minute boundaries");
        }
        if (durationMinutes < 30 || durationMinutes % 30 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service duration is not aligned with appointment slots");
        }
    }

    private void validateStaffStatusTransition(AppointmentStatus current, AppointmentStatus target) {
        if (target == AppointmentStatus.CANCELLED) {
            return;
        }
        if (current == AppointmentStatus.REQUESTED && target == AppointmentStatus.APPROVED) {
            return;
        }
        if (current == AppointmentStatus.APPROVED && target == AppointmentStatus.COMPLETED) {
            return;
        }
        if (current == target) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status transition");
    }
}
