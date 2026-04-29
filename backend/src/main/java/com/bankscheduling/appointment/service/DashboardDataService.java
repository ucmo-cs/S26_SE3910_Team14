package com.bankscheduling.appointment.service;

import com.bankscheduling.appointment.dto.appointment.StaffAppointmentUpdateRequest;
import com.bankscheduling.appointment.dto.dashboard.DashboardActivityDto;
import com.bankscheduling.appointment.dto.dashboard.DashboardAppointmentDto;
import com.bankscheduling.appointment.dto.dashboard.DashboardUserDto;
import com.bankscheduling.appointment.entity.Appointment;
import com.bankscheduling.appointment.entity.AppointmentStatus;
import com.bankscheduling.appointment.entity.AuditLog;
import com.bankscheduling.appointment.entity.BranchBusinessHours;
import com.bankscheduling.appointment.entity.User;
import com.bankscheduling.appointment.repository.AppointmentRepository;
import com.bankscheduling.appointment.repository.AuditLogRepository;
import com.bankscheduling.appointment.repository.BranchBusinessHoursRepository;
import com.bankscheduling.appointment.repository.UserRepository;
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
import java.time.Duration;
import java.util.List;

@Service
public class DashboardDataService {
    private final AppointmentRepository appointmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final BranchBusinessHoursRepository branchBusinessHoursRepository;
    private final AppointmentSlotInventoryService appointmentSlotInventoryService;

    public DashboardDataService(
            AppointmentRepository appointmentRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            BranchBusinessHoursRepository branchBusinessHoursRepository,
            AppointmentSlotInventoryService appointmentSlotInventoryService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.branchBusinessHoursRepository = branchBusinessHoursRepository;
        this.appointmentSlotInventoryService = appointmentSlotInventoryService;
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
        return userRepository.findAllWithRole().stream()
                .map(account -> new DashboardUserDto(
                        account.getId(),
                        account.getFullName(),
                        account.getEmailNormalized(),
                        normalizeRole(account.getRole().getName()),
                        !account.isActive()
                ))
                .toList();
    }

    @Transactional
    public void setUserLock(Long accountId, boolean locked) {
        requireAnyRole("ROLE_ADMIN");
        User account = userRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
        account.setActive(!locked);
        userRepository.save(account);
    }

    @Transactional
    public DashboardAppointmentDto updateAppointmentForStaff(Long appointmentId, StaffAppointmentUpdateRequest request) {
        requireAnyRole("ROLE_EMPLOYEE", "ROLE_ADMIN");
        Appointment appointment = appointmentRepository.findByIdWithAssociations(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        ZoneId branchZone = ZoneId.of(appointment.getBranch().getTimeZone());
        ZonedDateTime currentStart = appointment.getScheduledStart().atZone(branchZone);
        int duration = (int) Duration.between(appointment.getScheduledStart(), appointment.getScheduledEnd()).toMinutes();
        if (duration < 30 || duration % 30 != 0) {
            duration = appointment.getServiceType().getDefaultDurationMinutes();
        }
        boolean hasDate = request.date() != null;
        boolean hasStartTime = request.startTime() != null;
        if (hasDate != hasStartTime) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date and start time must be provided together");
        }
        java.time.LocalDate effectiveDate = hasDate ? request.date() : currentStart.toLocalDate();
        java.time.LocalTime effectiveStartTime = hasStartTime ? request.startTime() : currentStart.toLocalTime();
        ZonedDateTime start = ZonedDateTime.of(effectiveDate, effectiveStartTime, branchZone);
        ZonedDateTime end = start.plusMinutes(duration);
        boolean scheduleChanged = start.toInstant().compareTo(appointment.getScheduledStart()) != 0;

        if (scheduleChanged) {
        BranchBusinessHours hours = branchBusinessHoursRepository.findByBranchIdAndDayOfWeekAndActiveTrue(
                appointment.getBranch().getId(),
                effectiveDate.getDayOfWeek().getValue()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is closed on the selected date"));
        if (effectiveStartTime.isBefore(hours.getOpenTime()) || end.toLocalTime().isAfter(hours.getCloseTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment is outside branch business hours");
        }
        validateNineToFiveWindow(effectiveStartTime, end.toLocalTime());
        validateSlotBoundary(effectiveStartTime, duration);
        if (start.toInstant().isBefore(java.time.Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book an appointment in the past");
        }
        if (appointmentRepository.existsActiveBranchServiceOverlapExcluding(
                appointment.getBranch().getId(),
                appointment.getServiceType().getId(),
                appointment.getId(),
                start.toInstant(),
                end.toInstant()
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
        }

        AppointmentStatus previousStatus = appointment.getStatus();
        if (request.status() != null && !request.status().isBlank()) {
            try {
                AppointmentStatus targetStatus = AppointmentStatus.valueOf(request.status().trim().toUpperCase());
                validateStaffStatusTransition(appointment.getStatus(), targetStatus);
                appointment.setStatus(targetStatus);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid appointment status");
            }
        }
        if (scheduleChanged) {
            appointment.setScheduledStart(start.toInstant());
            appointment.setScheduledEnd(end.toInstant());
        }
        appointment.setNotes(request.notes());
        Appointment saved;
        try {
            saved = appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected timeslot is no longer available");
        }
        boolean statusChanged = previousStatus != saved.getStatus();
        if (scheduleChanged) {
            appointmentSlotInventoryService.releaseSlots(saved.getId());
            if (saved.getStatus() != AppointmentStatus.CANCELLED) {
                appointmentSlotInventoryService.reserveSlots(
                        saved,
                        saved.getBranch().getId(),
                        saved.getServiceType().getId(),
                        effectiveDate,
                        effectiveStartTime,
                        duration
                );
            }
        } else if (statusChanged && saved.getStatus() == AppointmentStatus.CANCELLED) {
            appointmentSlotInventoryService.releaseSlots(saved.getId());
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
        appointmentSlotInventoryService.releaseSlots(appointmentId);
    }

    private static String normalizeRole(String raw) {
        if (raw == null) {
            return "CUSTOMER";
        }
        return raw.startsWith("ROLE_") ? raw.substring("ROLE_".length()) : raw;
    }

    private static String formatAuditUser(AuditLog item) {
        if (item.getActorEmail() != null && !item.getActorEmail().isBlank()) {
            return item.getActorEmail();
        }
        if (item.getActorEmployeeId() != null) {
            String username = item.getActorUsername() == null ? "unknown-user" : item.getActorUsername();
            String email = item.getActorEmail() == null ? "n/a" : item.getActorEmail();
            String role = item.getActorRole() == null ? "n/a" : item.getActorRole();
            return username + " (" + email + ", " + role + ", emp#" + item.getActorEmployeeId() + ")";
        }
        if (item.getPerformedBy() != null) {
            return item.getPerformedBy().getEmailNormalized();
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
            builder.append(" | browser=").append(parseBrowser(item.getUserAgent()));
            builder.append(" | os=").append(parseOperatingSystem(item.getUserAgent()));
            builder.append(" | device=").append(parseDeviceType(item.getUserAgent()));
            builder.append(" | ua=").append(item.getUserAgent());
        }
        return builder.toString();
    }

    private static String parseBrowser(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) {
            return "Microsoft Edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        }
        if (ua.contains("chrome/") && !ua.contains("edg/") && !ua.contains("opr/")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        }
        if (ua.contains("trident/") || ua.contains("msie")) {
            return "Internet Explorer";
        }
        return "Unknown";
    }

    private static String parseOperatingSystem(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows nt")) {
            return "Windows";
        }
        if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "Unknown";
    }

    private static String parseDeviceType(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) {
            return "Mobile";
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "Tablet";
        }
        return "Desktop";
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
